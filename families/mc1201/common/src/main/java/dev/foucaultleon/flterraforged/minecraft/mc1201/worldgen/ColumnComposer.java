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

    /** Creates a column composer for the generator's vertical settings. */
    public ColumnComposer(
            int minY,
            int maxYExclusive,
            int seaLevel,
            BlockState baseState,
            BlockState defaultFluid) {
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
        this.seaLevel = seaLevel;
        this.baseState = baseState;
        this.defaultFluid = defaultFluid;
    }

    /** Returns the first air block above the solid surface. */
    public int surfaceTop(TerrainSample sample) {
        return clamp((int) Math.floor(sample.surfaceHeight()) + 1, minY + 1, maxYExclusive);
    }

    /** Builds a full vertical column suitable for chunk fill and structure sampling. */
    public BlockState[] compose(TerrainSample sample) {
        BlockState[] states = new BlockState[maxYExclusive - minY];
        int surfaceTop = surfaceTop(sample);
        int surfaceY = surfaceTop - 1;
        int waterTopExclusive = waterTopExclusive(sample, surfaceTop);
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

    private int waterTopExclusive(TerrainSample sample, int surfaceTop) {
        // Keep synchronous column sampling consistent with EngineDensityBridge:
        // only the global sea level is materialized until a stable river-water
        // level is part of the engine contract.
        return clamp(Math.max(surfaceTop, seaLevel + 1), minY, maxYExclusive);
    }

    private BlockState topState(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || StandardTerrainTypes.COAST.equals(terrain)
                || StandardTerrainTypes.RIVER.equals(terrain)) {
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
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || StandardTerrainTypes.COAST.equals(terrain)
                || StandardTerrainTypes.RIVER.equals(terrain)) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.DIRT.getDefaultState();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
