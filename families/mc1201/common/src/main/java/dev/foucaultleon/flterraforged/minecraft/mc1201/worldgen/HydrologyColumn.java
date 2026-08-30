package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/** Shared block-column realization rules for engine-provided water surfaces. */
final class HydrologyColumn {

    private static final double MIN_WET_DEPTH = 0.05D;

    private HydrologyColumn() {
    }

    /**
     * Returns the first block above all water that belongs to this terrain column.
     *
     * <p>Global sea-level filling is retained for compatibility with the current ocean model. A
     * river may raise the local water top above sea level only when the engine exposes a finite,
     * continuous river-water surface and the channel is actually incised at this X/Z position.</p>
     */
    static int waterTopExclusive(
            TerrainSample sample,
            int solidSurfaceTop,
            int seaLevel,
            int minY,
            int maxYExclusive) {
        int waterTop = Math.max(solidSurfaceTop, seaLevel + 1);
        RiverSample river = sample.river();
        if (river.hasWaterSurfaceHeight()
                && river.depth() > MIN_WET_DEPTH
                && river.waterSurfaceHeight() > sample.surfaceHeight()) {
            int riverTop = (int) Math.floor(river.waterSurfaceHeight()) + 1;
            waterTop = Math.max(waterTop, riverTop);
        }
        return clamp(waterTop, minY, maxYExclusive);
    }

    /** Returns whether at least one full water block can exist above the solid river bed. */
    static boolean hasMaterializedRiverWater(TerrainSample sample) {
        RiverSample river = sample.river();
        if (!river.hasWaterSurfaceHeight() || river.depth() <= MIN_WET_DEPTH) {
            return false;
        }
        int bedTop = (int) Math.floor(sample.surfaceHeight()) + 1;
        int waterTop = (int) Math.floor(river.waterSurfaceHeight()) + 1;
        return waterTop > bedTop;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
