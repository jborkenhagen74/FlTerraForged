package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerCapabilities;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.RiparianZone;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Standard full-block materializer shipped with FlTerraForged for Minecraft 1.20.1. */
public final class VanillaBlockMaterializer implements BlockMaterializer {

    private static final double MIN_WET_DEPTH = 0.05D;
    private static final MaterializerCapabilities CAPABILITIES =
            new MaterializerCapabilities(1.0D, false, false);

    private final MaterializerContext context;

    /**
     * Creates the vanilla-compatible full-block implementation.
     *
     * @param context active generation context
     */
    public VanillaBlockMaterializer(MaterializerContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public MaterializerContext context() {
        return context;
    }

    @Override
    public MaterializerCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public int solidSurfaceY(TerrainSample sample) {
        int surfaceY = clamp(
                (int) Math.floor(sample.surfaceHeight()),
                context.minY() + 1,
                context.maxYExclusive() - 2);
        RiverSample hydrology = sample.river();
        if (StandardTerrainTypes.LAKE.equals(sample.terrainType())
                && hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH) {
            int waterTopExclusive = clamp(
                    (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                    context.minY() + 1,
                    context.maxYExclusive());
            surfaceY = Math.min(surfaceY, waterTopExclusive - 2);
        }
        return clamp(surfaceY, context.minY() + 1, context.maxYExclusive() - 2);
    }

    @Override
    public int waterTopExclusive(TerrainSample sample) {
        int solidTop = solidSurfaceTop(sample);
        int waterTop = Math.max(solidTop, context.seaLevel() + 1);
        RiverSample hydrology = sample.river();
        if (hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH
                && hydrology.waterSurfaceHeight() > sample.surfaceHeight()) {
            int localWaterTop = (int) Math.floor(hydrology.waterSurfaceHeight()) + 1;
            waterTop = Math.max(waterTop, localWaterTop);
        }
        return clamp(waterTop, context.minY(), context.maxYExclusive());
    }

    @Override
    public boolean hasMaterializedWater(TerrainSample sample) {
        RiverSample hydrology = sample.river();
        if (!hydrology.hasWaterSurfaceHeight() || hydrology.depth() <= MIN_WET_DEPTH) {
            return false;
        }
        int bedTop = solidSurfaceTop(sample);
        int waterTop = clamp(
                (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                context.minY(),
                context.maxYExclusive());
        return waterTop > bedTop;
    }

    @Override
    public BlockState bedrockState(TerrainSample sample) {
        return Blocks.BEDROCK.getDefaultState();
    }

    @Override
    public BlockState substrateState(TerrainSample sample) {
        return context.defaultBlock();
    }

    @Override
    public BlockState surfaceSealState(TerrainSample sample) {
        return context.defaultBlock();
    }

    @Override
    public BlockState airState(TerrainSample sample) {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public BlockState fluidState(TerrainSample sample) {
        return context.defaultFluid();
    }

    @Override
    public BlockState composedTopState(TerrainSample sample) {
        return forcedSurfaceState(sample).orElseGet(() -> fallbackSurfaceState(sample));
    }

    @Override
    public BlockState fillerState(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return Blocks.GRAVEL.getDefaultState();
        }
        if (RiparianZone.isDryBank(sample)) {
            return Blocks.DIRT.getDefaultState();
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(terrain)) {
            return dryShore(sample)
                    ? Blocks.SAND.getDefaultState()
                    : Blocks.DIRT.getDefaultState();
        }
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || StandardTerrainTypes.COAST.equals(terrain)) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.DIRT.getDefaultState();
    }

    @Override
    public Optional<BlockState> forcedSurfaceState(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return Optional.of(Blocks.SAND.getDefaultState());
        }
        if ((StandardTerrainTypes.RIVER.equals(terrain)
                        || StandardTerrainTypes.LAKE.equals(terrain))
                && hasMaterializedWater(sample)) {
            return Optional.of(Blocks.GRAVEL.getDefaultState());
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(terrain)) {
            return Optional.of(dryShore(sample)
                    ? Blocks.SAND.getDefaultState()
                    : Blocks.GRASS_BLOCK.getDefaultState());
        }
        if (RiparianZone.isDryBank(sample)) {
            return Optional.of(Blocks.GRASS_BLOCK.getDefaultState());
        }
        if (sample.climate().isAvailable()
                && sample.climate().temperature() < 0.20D
                && sample.surfaceHeight() > context.seaLevel() + 4) {
            return Optional.of(Blocks.SNOW_BLOCK.getDefaultState());
        }
        return Optional.empty();
    }

    @Override
    public BlockState fallbackSurfaceState(TerrainSample sample) {
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.GRASS_BLOCK.getDefaultState();
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample) {
        return Blocks.GRAVEL.getDefaultState();
    }

    @Override
    public BlockState hydrologySealState(TerrainSample sample) {
        return context.defaultBlock();
    }

    private static boolean dryShore(TerrainSample sample) {
        return sample.climate().isAvailable()
                && (sample.climate().temperature() > 0.72D
                        || sample.climate().moisture() < 0.30D);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
