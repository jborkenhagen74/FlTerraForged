package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.chunk.Chunk;

/**
 * First Minecraft 1.20.1 chunk-generator adapter backed by FlTerraForged Engine.
 *
 * <p>This reference adapter intentionally fills simple solid/water columns. It
 * proves engine seed binding, height generation, native biome routing and data
 * driven world-preset loading before vanilla caves/surface rules are integrated.</p>
 */
public final class FlTerraForgedChunkGenerator extends ChunkGenerator {

    /** Default external engine provider. */
    public static final String DEFAULT_ENGINE = "flterraforged:default";

    /** Codec registered in Minecraft's chunk-generator type registry. */
    public static final Codec<FlTerraForgedChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(FlTerraForgedChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings")
                            .forGetter(FlTerraForgedChunkGenerator::settings),
                    Codec.STRING.optionalFieldOf("engine", DEFAULT_ENGINE)
                            .forGetter(FlTerraForgedChunkGenerator::engineId),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("engine_config", Map.of("preset", "balanced"))
                            .forGetter(FlTerraForgedChunkGenerator::engineConfig)
            ).apply(instance, FlTerraForgedChunkGenerator::new));

    private static final Set<Heightmap.Type> GENERATED_HEIGHTMAPS = Set.of(
            Heightmap.Type.OCEAN_FLOOR_WG,
            Heightmap.Type.WORLD_SURFACE_WG);

    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final String engineId;
    private final Map<String, String> engineConfig;
    private final FlTerraForgedBiomeSource engineBiomeSource;
    private final EngineWorldSession session;
    private final ColumnComposer columns;

    /** Creates a data-driven generator from the registered codec. */
    public FlTerraForgedChunkGenerator(
            BiomeSource biomeSource,
            RegistryEntry<ChunkGeneratorSettings> settings,
            String engineId,
            Map<String, String> engineConfig) {
        super(biomeSource);
        if (!(biomeSource instanceof FlTerraForgedBiomeSource source)) {
            throw new IllegalArgumentException(
                    "FlTerraForgedChunkGenerator requires flterraforged:biome_source");
        }
        this.engineBiomeSource = source;
        this.settings = Objects.requireNonNull(settings, "settings");
        this.engineId = Objects.requireNonNull(engineId, "engineId");
        this.engineConfig = Map.copyOf(Objects.requireNonNull(engineConfig, "engineConfig"));

        ChunkGeneratorSettings value = settings.value();
        GenerationShapeConfig shape = value.generationShapeConfig();
        int minY = shape.minimumY();
        int maxYExclusive = minY + shape.height();
        this.session = new EngineWorldSession(
                engineId, this.engineConfig, minY, maxYExclusive, value.seaLevel());
        this.columns = new ColumnComposer(
                minY,
                maxYExclusive,
                value.seaLevel(),
                value.defaultBlock(),
                value.defaultFluid());
    }

    /** Returns the configured vanilla chunk-generator settings entry. */
    public RegistryEntry<ChunkGeneratorSettings> settings() {
        return settings;
    }

    /** Returns the external engine provider identifier. */
    public String engineId() {
        return engineId;
    }

    /** Returns the immutable engine configuration. */
    public Map<String, String> engineConfig() {
        return engineConfig;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateBiomes(
            Executor executor,
            NoiseConfig noiseConfig,
            Blender blender,
            StructureAccessor structureAccessor,
            Chunk chunk) {
        bind(noiseConfig);
        return super.populateBiomes(executor, noiseConfig, blender, structureAccessor, chunk);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Executor executor,
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structureAccessor,
            Chunk chunk) {
        TerrainWorld world = bind(noiseConfig);
        return CompletableFuture.supplyAsync(() -> {
            fillChunk(chunk, world);
            Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS);
            return chunk;
        }, executor);
    }

    @Override
    public int getHeight(
            int x,
            int z,
            Heightmap.Type heightmap,
            HeightLimitView world,
            NoiseConfig noiseConfig) {
        TerrainSample sample = bind(noiseConfig).sample(x, z);
        return columns.surfaceTop(sample);
    }

    @Override
    public VerticalBlockSample getColumnSample(
            int x,
            int z,
            HeightLimitView world,
            NoiseConfig noiseConfig) {
        TerrainSample sample = bind(noiseConfig).sample(x, z);
        return new VerticalBlockSample(getMinimumY(), columns.compose(sample));
    }

    @Override
    public int getMinimumY() {
        return settings.value().generationShapeConfig().minimumY();
    }

    @Override
    public int getWorldHeight() {
        return settings.value().generationShapeConfig().height();
    }

    @Override
    public int getSeaLevel() {
        return settings.value().seaLevel();
    }

    @Override
    public void buildSurface(
            ChunkRegion region,
            StructureAccessor structures,
            NoiseConfig noiseConfig,
            Chunk chunk) {
        // The first reference adapter composes its basic surface while filling columns.
    }

    @Override
    public void carve(
            ChunkRegion chunkRegion,
            long seed,
            NoiseConfig noiseConfig,
            BiomeAccess biomeAccess,
            StructureAccessor structureAccessor,
            Chunk chunk,
            GenerationStep.Carver carverStep) {
        // Vanilla cave/carver delegation is intentionally a follow-up integration step.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Entity population is intentionally left to a later vanilla-delegation pass.
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        TerrainSample sample = bind(noiseConfig).sample(pos.getX(), pos.getZ());
        text.add("FlTerraForged engine: " + session.providerId());
        text.add(String.format(
                java.util.Locale.ROOT,
                "FTF h=%.2f slope=%.3f erosion=%.3f continent=%.3f terrain=%s",
                sample.surfaceHeight(),
                sample.slope(),
                sample.erosion(),
                sample.continentalness(),
                sample.terrainType()));
    }

    private TerrainWorld bind(NoiseConfig noiseConfig) {
        TerrainWorld world = session.bind(noiseConfig);
        engineBiomeSource.bind(world);
        return world;
    }

    private void fillChunk(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        int startX = pos.getStartX();
        int startZ = pos.getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; localZ++) {
            int blockZ = startZ + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int blockX = startX + localX;
                TerrainSample sample = world.sample(blockX, blockZ);
                BlockState[] states = columns.compose(sample);
                for (int index = 0; index < states.length; index++) {
                    BlockState state = states[index];
                    if (state.isAir()) {
                        continue;
                    }
                    int y = getMinimumY() + index;
                    mutable.set(blockX, y, blockZ);
                    chunk.setBlockState(mutable, state, false);
                }
            }
        }
    }
}
