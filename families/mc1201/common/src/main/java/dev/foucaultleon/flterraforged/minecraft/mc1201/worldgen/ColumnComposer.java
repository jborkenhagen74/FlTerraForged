package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Composes the first block-column representation from an engine terrain sample. */
public final class ColumnComposer {

    private static final int FILLER_DEPTH = 3;

    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;
    private final BlockState baseState;
    private final BlockState defaultFluid;
    private final TerrainMaterializer materializer;

    /**
     * Creates a column composer for the generator's vertical settings.
     *
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @param seaLevel world sea level
     * @param baseState default solid substrate
     * @param defaultFluid default water/fluid state
     * @param materializer active vertical-resolution materializer
     */
    public ColumnComposer(
            int minY,
            int maxYExclusive,
            int seaLevel,
            BlockState baseState,
            BlockState defaultFluid,
            TerrainMaterializer materializer) {
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
        this.seaLevel = seaLevel;
        this.baseState = baseState;
        this.defaultFluid = defaultFluid;
        this.materializer = materializer;
    }

    /**
     * Returns the first air block above the materialized solid surface.
     *
     * @param sample continuous Engine terrain sample
     * @return first block above the solid surface
     */
    public int surfaceTop(TerrainSample sample) {
        return materializer.solidSurfaceTop(sample, minY, maxYExclusive);
    }

    /**
     * Returns the first block above solid terrain plus ocean or Engine hydrology water.
     *
     * @param sample continuous Engine terrain sample
     * @return first block above the complete world surface
     */
    public int worldSurfaceTop(TerrainSample sample) {
        return materializer.waterTopExclusive(sample, seaLevel, minY, maxYExclusive);
    }

    /**
     * Builds a full vertical column suitable for chunk fill and structure sampling.
     *
     * @param sample continuous Engine terrain sample
     * @return block states from {@code minY} through the exclusive maximum Y
     */
    public BlockState[] compose(TerrainSample sample) {
        BlockState[] states = new BlockState[maxYExclusive - minY];
        int surfaceTop = surfaceTop(sample);
        int surfaceY = surfaceTop - 1;
        int waterTopExclusive = materializer.waterTopExclusive(
                sample, seaLevel, minY, maxYExclusive);
        BlockState top = topState(sample);
        BlockState filler = fillerState(sample);

        for (int y = minY; y < maxYExclusive; y++) {
            BlockState state;
            if (y == minY) {
                state = Blocks.BEDROCK.getDefaultState();
            } else if (y < surfaceY - FILLER_DEPTH + 1) {
                state = baseState;
            } else if (y < surfaceY) {
                state = filler;
            } else if (y == surfaceY) {
                state = top;
            } else if (y < waterTopExclusive) {
                state = defaultFluid;
            } else {
                state = Blocks.AIR.getDefaultState();
            }
            states[y - minY] = state;
        }
        return states;
    }

    private BlockState topState(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return Blocks.GRAVEL.getDefaultState();
        }
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || StandardTerrainTypes.COAST.equals(terrain)) {
            return Blocks.SAND.getDefaultState();
        }
        if (sample.climate().isAvailable()
                && sample.climate().temperature() < 0.20
                && sample.surfaceHeight() > seaLevel + 4) {
            return Blocks.SNOW_BLOCK.getDefaultState();
        }
        return Blocks.GRASS_BLOCK.getDefaultState();
    }

    private BlockState fillerState(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return Blocks.GRAVEL.getDefaultState();
        }
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || StandardTerrainTypes.COAST.equals(terrain)) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.DIRT.getDefaultState();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
