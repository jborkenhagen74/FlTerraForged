package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.WaterDecorationContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.chunk.ChunkSnapshot;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.MaterializerRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/**
 * Minecraft 1.20.1 adapter for the Engine-owned FlTerraForged natural world.
 *
 * <p>The external Engine is the sole owner of natural terrain and subsurface geometry, including
 * surface shape, hydrology, geology, caves, ravines, underground fluids and the world floor.
 * Minecraft's chunk-status pipeline remains the scheduler, registry and feature/structure host,
 * but Vanilla noise terrain, surface rules, carvers and aquifers are intentionally absent from the
 * natural terrain hot path. The active replaceable materializer maps one immutable Engine
 * {@link ChunkSnapshot} directly to Minecraft blocks.</p>
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
                            .forGetter(FlTerraForgedChunkGenerator::engineConfig))
            .apply(instance, FlTerraForgedChunkGenerator::new));

    private static final Set<Heightmap.Type> GENERATED_HEIGHTMAPS = Set.of(
            Heightmap.Type.OCEAN_FLOOR_WG,
            Heightmap.Type.WORLD_SURFACE_WG);

    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final String engineId;
    private final Map<String, String> engineConfig;
    private final FlTerraForgedBiomeSource engineBiomeSource;
    private final EngineWorldSession session;
    private final BlockMaterializer materializer;
    private final ColumnComposer columns;
    private final EngineChunkMaterializer chunkMaterializer;
    private final MarineEnvironmentCache marineEnvironmentCache;

    /**
     * Creates a data-driven generator from the registered codec.
     *
     * @param biomeSource FlTerraForged biome source
     * @param settings Minecraft world-height, sea-level and feature settings
     * @param engineId configured Engine provider identifier
     * @param engineConfig immutable Engine configuration
     */
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
        MaterializerContext materializerContext = new MaterializerContext(
                minY,
                maxYExclusive,
                value.seaLevel(),
                value.defaultBlock(),
                value.defaultFluid(),
                MaterializerRuntime.options());
        this.materializer = MaterializerRuntime.create(materializerContext);
        this.columns = new ColumnComposer(materializer);
        this.chunkMaterializer = new EngineChunkMaterializer(materializer);
        this.marineEnvironmentCache = new MarineEnvironmentCache(materializer);
    }

    /** Returns the configured Minecraft chunk-generator settings entry. */
    public RegistryEntry<ChunkGeneratorSettings> settings() {
        return settings;
    }

    /** Returns the external Engine provider identifier. */
    public String engineId() {
        return engineId;
    }

    /** Returns the immutable Engine configuration. */
    public Map<String, String> engineConfig() {
        return engineConfig;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(
            RegistryWrapper<StructureSet> structureSetRegistry,
            NoiseConfig noiseConfig,
            long seed) {
        bind(noiseConfig);
        engineBiomeSource.beginStructureSampling();
        try {
            return super.createStructurePlacementCalculator(structureSetRegistry, noiseConfig, seed);
        } finally {
            engineBiomeSource.endStructureSampling();
        }
    }

    @Override
    public void setStructureStarts(
            DynamicRegistryManager registryManager,
            StructurePlacementCalculator placementCalculator,
            StructureAccessor structureAccessor,
            Chunk chunk,
            StructureTemplateManager structureTemplateManager) {
        engineBiomeSource.beginStructureSampling();
        try {
            super.setStructureStarts(
                    registryManager,
                    placementCalculator,
                    structureAccessor,
                    chunk,
                    structureTemplateManager);
        } finally {
            engineBiomeSource.endStructureSampling();
        }

        Map<Structure, StructureStart> retained = new HashMap<>(chunk.getStructureStarts());
        var structureRegistry = registryManager.get(RegistryKeys.STRUCTURE);
        int centerX = chunk.getPos().getCenterX();
        int centerZ = chunk.getPos().getCenterZ();
        TerrainWorld terrainWorld = session.boundWorld();
        boolean changed = retained.entrySet().removeIf(entry -> {
            var id = structureRegistry.getId(entry.getKey());
            return id != null && !MarineStructureGuard.permits(
                    id.toString(),
                    entry.getValue().hasChildren(),
                    centerX,
                    centerZ,
                    terrainWorld,
                    marineEnvironmentCache);
        });
        if (changed) {
            chunk.setStructureStarts(retained);
        }
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
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        return CompletableFuture.supplyAsync(() -> {
            ChunkSnapshot snapshot = world.chunkSnapshot(chunkX, chunkZ);
            chunkMaterializer.materialize(chunk, snapshot);
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
        if (heightmap == Heightmap.Type.OCEAN_FLOOR
                || heightmap == Heightmap.Type.OCEAN_FLOOR_WG) {
            return columns.surfaceTop(sample);
        }
        return columns.worldSurfaceTop(sample);
    }

    @Override
    public VerticalBlockSample getColumnSample(
            int x,
            int z,
            HeightLimitView world,
            NoiseConfig noiseConfig) {
        TerrainSample sample = bind(noiseConfig).sample(x, z);
        return new VerticalBlockSample(getMinimumY(), columns.compose(sample, x, z));
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

    /**
     * The complete natural surface is already present after {@link #populateNoise}; Vanilla surface
     * rules must not alter Engine-owned geometry.
     */
    @Override
    public void buildSurface(
            ChunkRegion region,
            StructureAccessor structures,
            NoiseConfig noiseConfig,
            Chunk chunk) {
        bind(noiseConfig);
        Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS);
    }

    /**
     * Caves, ravines and underground fluids are resolved in the Engine snapshot. Vanilla carvers
     * are deliberately disabled so they cannot create a second subsurface truth.
     */
    @Override
    public void carve(
            ChunkRegion chunkRegion,
            long seed,
            NoiseConfig noiseConfig,
            BiomeAccess biomeAccess,
            StructureAccessor structureAccessor,
            Chunk chunk,
            GenerationStep.Carver carverStep) {
        bind(noiseConfig);
    }

    /**
     * Runs Minecraft's biome-driven mob population after Engine-owned natural geometry exists.
     * This stage does not create or modify terrain.
     */
    @Override
    public void populateEntities(ChunkRegion region) {
        if (settings.value().mobGenerationDisabled()) {
            return;
        }
        ChunkPos chunkPos = region.getCenterPos();
        var biome = region.getBiome(chunkPos.getStartPos().withY(region.getTopY() - 1));
        ChunkRandom random = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        random.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
        SpawnHelper.populateEntities(region, biome, chunkPos, random);
    }

    @Override
    public void generateFeatures(
            StructureWorldAccess world,
            Chunk chunk,
            StructureAccessor structureAccessor) {
        super.generateFeatures(world, chunk, structureAccessor);
        materializer.decorateWatercourses(new WaterDecorationContext(
                world,
                chunk,
                session.boundWorld()));
        Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        TerrainSample sample = bind(noiseConfig).sample(pos.getX(), pos.getZ());
        text.add("FlTerraForged engine: " + session.providerId() + " @ " + session.providerVersion());
        text.add(String.format(
                java.util.Locale.ROOT,
                "FTF materializer=%s resolution=%.2f partial=%s waterlogging=%s marineCols=%d marineSummaries=%d",
                MaterializerRuntime.selectedId(),
                materializer.capabilities().verticalResolution(),
                materializer.capabilities().partialBlocks(),
                materializer.capabilities().waterlogging(),
                marineEnvironmentCache.cachedColumns(),
                marineEnvironmentCache.cachedSummaries()));
        text.add(String.format(
                java.util.Locale.ROOT,
                "FTF h=%.2f slope=%.3f erosion=%.3f continent=%.3f terrain=%s",
                sample.surfaceHeight(),
                sample.slope(),
                sample.erosion(),
                sample.continentalness(),
                sample.terrainType()));
        if (sample.river().hasWaterSurfaceHeight()) {
            text.add(String.format(
                    java.util.Locale.ROOT,
                    "FTF river d=%.2f w=%.2f water=%.2f flow=%.2f wet=%s",
                    sample.river().depth(),
                    sample.river().width(),
                    sample.river().waterSurfaceHeight(),
                    sample.river().flow(),
                    materializer.hasMaterializedWater(sample)));
        }
    }

    private TerrainWorld bind(NoiseConfig noiseConfig) {
        TerrainWorld world = session.bind(noiseConfig);
        engineBiomeSource.bind(world, getSeaLevel());
        return world;
    }
}
