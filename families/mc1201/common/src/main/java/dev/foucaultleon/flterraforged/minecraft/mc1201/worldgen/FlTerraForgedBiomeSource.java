package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

/** Minecraft 1.20.1 biome source backed by the external terrain engine. */
public final class FlTerraForgedBiomeSource extends BiomeSource {

    /** Codec registered in Minecraft's biome-source type registry. */
    public static final Codec<FlTerraForgedBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomePalette.CODEC.fieldOf("palette").forGetter(FlTerraForgedBiomeSource::palette)
            ).apply(instance, FlTerraForgedBiomeSource::new));

    private static final int STRUCTURE_SAMPLE_CELL_SIZE = 128;
    private static final int MAXIMUM_STRUCTURE_BIOME_ENTRIES = 2048;

    private final BiomePalette palette;
    private final ThreadLocal<Integer> structureSamplingDepth = ThreadLocal.withInitial(() -> 0);
    private volatile Binding binding;

    /** Creates an unbound biome source from a native biome palette. */
    public FlTerraForgedBiomeSource(BiomePalette palette) {
        this.palette = Objects.requireNonNull(palette, "palette");
    }

    /**
     * Binds this biome source to the same engine world used by the chunk generator.
     *
     * @param terrainWorld active engine world
     * @param seaLevel active Minecraft sea level used for shallow/deep ocean roles
     */
    public synchronized void bind(TerrainWorld terrainWorld, int seaLevel) {
        TerrainWorld requested = Objects.requireNonNull(terrainWorld, "terrainWorld");
        Binding current = binding;
        if (current != null && current.world == requested && current.seaLevel == seaLevel) {
            return;
        }
        binding = new Binding(requested, seaLevel);
    }

    /** Compatibility overload using the vanilla Overworld sea level. */
    public void bind(TerrainWorld terrainWorld) {
        bind(terrainWorld, 63);
    }

    /** Returns the serialized palette. */
    public BiomePalette palette() {
        return palette;
    }

    /** Marks the current thread as executing a vanilla structure-placement biome query. */
    void beginStructureSampling() {
        structureSamplingDepth.set(structureSamplingDepth.get() + 1);
    }

    /** Ends one current-thread structure-placement sampling scope. */
    void endStructureSampling() {
        int depth = structureSamplingDepth.get();
        if (depth <= 1) {
            structureSamplingDepth.remove();
        } else {
            structureSamplingDepth.set(depth - 1);
        }
    }

    /**
     * Marks one chunk as being filled by Minecraft's asynchronous biome status.
     *
     * <p>Biome filling only needs broad climate/land semantics. R60 therefore routes those queries
     * through the cheap placement sampler and a tiny per-chunk result cache. Exact erosion,
     * hydrology and subsurface work remains deferred to the later Engine snapshot stage.</p>
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     */
    void beginBiomePopulation(int chunkX, int chunkZ) {
        Binding current = requireBinding();
        long chunkKey = key(chunkX, chunkZ);
        current.biomePopulation.compute(chunkKey, (ignored, existing) -> {
            if (existing == null) {
                return new BiomePopulationScope();
            }
            existing.users.incrementAndGet();
            return existing;
        });
    }

    /** Ends one asynchronous biome-population scope. */
    void endBiomePopulation(int chunkX, int chunkZ) {
        Binding current = binding;
        if (current == null) {
            return;
        }
        long chunkKey = key(chunkX, chunkZ);
        current.biomePopulation.computeIfPresent(chunkKey, (ignored, scope) ->
                scope.users.decrementAndGet() <= 0 ? null : scope);
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return palette.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(
            int biomeX,
            int biomeY,
            int biomeZ,
            MultiNoiseUtil.MultiNoiseSampler noise) {
        Binding current = binding;
        if (current == null) {
            return palette.fallback();
        }
        int blockX = BiomeCoords.toBlock(biomeX);
        int blockZ = BiomeCoords.toBlock(biomeZ);
        if (structureSamplingDepth.get() > 0) {
            return structureBiome(current, blockX, blockZ);
        }

        long chunkKey = key(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
        BiomePopulationScope scope = current.biomePopulation.get(chunkKey);
        if (scope != null) {
            long pointKey = key(blockX, blockZ);
            return scope.biomes.computeIfAbsent(pointKey, ignored -> {
                TerrainSample sample = current.world.placementSample(blockX, blockZ);
                return NativeBiomeRouter.route(
                        sample, palette, current.seaLevel, blockX, blockZ, current.seed);
            });
        }

        TerrainSample sample = current.world.sample(blockX, blockZ);
        return NativeBiomeRouter.route(
                sample, palette, current.seaLevel, blockX, blockZ, current.seed);
    }

    private RegistryEntry<Biome> structureBiome(Binding current, int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, STRUCTURE_SAMPLE_CELL_SIZE);
        int cellZ = Math.floorDiv(blockZ, STRUCTURE_SAMPLE_CELL_SIZE);
        long key = key(cellX, cellZ);
        RegistryEntry<Biome> completed = current.structureBiomes.get(key);
        if (completed != null) {
            return completed;
        }

        CompletableFuture<RegistryEntry<Biome>> owned = new CompletableFuture<>();
        CompletableFuture<RegistryEntry<Biome>> existing = current.inFlight.putIfAbsent(key, owned);
        if (existing != null) {
            return await(existing);
        }
        try {
            int sampleX = Math.addExact(
                    Math.multiplyExact(cellX, STRUCTURE_SAMPLE_CELL_SIZE),
                    STRUCTURE_SAMPLE_CELL_SIZE / 2);
            int sampleZ = Math.addExact(
                    Math.multiplyExact(cellZ, STRUCTURE_SAMPLE_CELL_SIZE),
                    STRUCTURE_SAMPLE_CELL_SIZE / 2);
            TerrainSample sample = current.world.placementSample(sampleX, sampleZ);
            RegistryEntry<Biome> generated = NativeBiomeRouter.route(
                    sample, palette, current.seaLevel, sampleX, sampleZ, current.seed);
            RegistryEntry<Biome> retained = current.structureBiomes.putIfAbsent(key, generated);
            RegistryEntry<Biome> result = retained == null ? generated : retained;
            if (retained == null) {
                current.insertionOrder.add(key);
                trim(current);
            }
            owned.complete(result);
            return result;
        } catch (Throwable throwable) {
            owned.completeExceptionally(throwable);
            throw propagate(throwable);
        } finally {
            current.inFlight.remove(key, owned);
        }
    }

    private Binding requireBinding() {
        Binding current = binding;
        if (current == null) {
            throw new IllegalStateException("Biome source must be bound before biome population");
        }
        return current;
    }

    private static void trim(Binding current) {
        while (current.structureBiomes.size() > MAXIMUM_STRUCTURE_BIOME_ENTRIES) {
            Long eldest = current.insertionOrder.poll();
            if (eldest == null) {
                return;
            }
            current.structureBiomes.remove(eldest);
        }
    }

    private static RegistryEntry<Biome> await(CompletableFuture<RegistryEntry<Biome>> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            throw propagate(cause == null ? exception : cause);
        }
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Structure-stage biome sampling failed", throwable);
    }

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static final class Binding {

        private final TerrainWorld world;
        private final int seaLevel;
        private final long seed;
        private final ConcurrentMap<Long, RegistryEntry<Biome>> structureBiomes =
                new ConcurrentHashMap<>();
        private final ConcurrentMap<Long, CompletableFuture<RegistryEntry<Biome>>> inFlight =
                new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<Long> insertionOrder = new ConcurrentLinkedQueue<>();
        private final ConcurrentMap<Long, BiomePopulationScope> biomePopulation =
                new ConcurrentHashMap<>();

        Binding(TerrainWorld world, int seaLevel) {
            this.world = world;
            this.seaLevel = seaLevel;
            this.seed = world.context().seed();
        }
    }

    private static final class BiomePopulationScope {
        private final AtomicInteger users = new AtomicInteger(1);
        private final ConcurrentMap<Long, RegistryEntry<Biome>> biomes = new ConcurrentHashMap<>();
    }
}
