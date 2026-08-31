package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/** Full-block terrain materializer used by the vanilla-compatible Minecraft 1.20.1 host. */
public final class VanillaTerrainMaterializer implements TerrainMaterializer {

    private static final double MIN_WET_DEPTH = 0.05D;

    /** Creates the full-block vanilla materializer. */
    public VanillaTerrainMaterializer() {
    }

    /** {@inheritDoc} */
    @Override
    public double verticalResolution() {
        return 1.0D;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPartialBlocks() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsWaterlogging() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int solidSurfaceY(TerrainSample sample, int minY, int maxYExclusive) {
        int surfaceY = clamp(
                (int) Math.floor(sample.surfaceHeight()),
                minY + 1,
                maxYExclusive - 2);
        RiverSample hydrology = sample.river();

        // A continuous lake can be shallower than one Minecraft block. In that case the Engine
        // correctly marks water, but a full-block host would otherwise quantize bed and water to
        // the same integer Y and leave a dry gravel patch. Lower only lake/pond beds enough to
        // guarantee one real water block; river channels retain their already profiled geometry.
        if (StandardTerrainTypes.LAKE.equals(sample.terrainType())
                && hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH) {
            int waterTopExclusive = clamp(
                    (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                    minY + 1,
                    maxYExclusive);
            surfaceY = Math.min(surfaceY, waterTopExclusive - 2);
        }
        return clamp(surfaceY, minY + 1, maxYExclusive - 2);
    }

    /** {@inheritDoc} */
    @Override
    public int waterTopExclusive(
            TerrainSample sample,
            int seaLevel,
            int minY,
            int maxYExclusive) {
        int solidTop = solidSurfaceTop(sample, minY, maxYExclusive);
        int waterTop = Math.max(solidTop, seaLevel + 1);
        RiverSample hydrology = sample.river();
        if (hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH
                && hydrology.waterSurfaceHeight() > sample.surfaceHeight()) {
            int localWaterTop = (int) Math.floor(hydrology.waterSurfaceHeight()) + 1;
            waterTop = Math.max(waterTop, localWaterTop);
        }
        return clamp(waterTop, minY, maxYExclusive);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasMaterializedWater(TerrainSample sample, int minY, int maxYExclusive) {
        RiverSample hydrology = sample.river();
        if (!hydrology.hasWaterSurfaceHeight() || hydrology.depth() <= MIN_WET_DEPTH) {
            return false;
        }
        int bedTop = solidSurfaceTop(sample, minY, maxYExclusive);
        int waterTop = clamp(
                (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                minY,
                maxYExclusive);
        return waterTop > bedTop;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
