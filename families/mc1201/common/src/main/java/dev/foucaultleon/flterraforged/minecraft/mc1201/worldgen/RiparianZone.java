package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/** Minecraft-family facade for shared dry-riparian predicates. */
public final class RiparianZone {

    private RiparianZone() {
    }

    /**
     * Returns whether the sample is a dry-climate riverbank rather than the wet channel itself.
     *
     * @param sample Engine terrain sample
     * @return {@code true} when the sample belongs to the dry-climate riparian fringe
     */
    public static boolean isDryBank(TerrainSample sample) {
        return BiomeClimateRouter.isDryRiparianBank(sample);
    }

    /**
     * Returns whether the sample belongs to the dry solid bank envelope of a linear watercourse.
     *
     * @param sample Engine terrain sample
     * @return {@code true} beside an active river but outside its materialized water column
     */
    public static boolean isRiverBank(TerrainSample sample) {
        var river = sample.river();
        if (!river.isAvailable() || !river.hasFlow() || !(river.flow() > 0.0D)) {
            return false;
        }
        if (river.hasWaterSurfaceHeight()
                && river.waterSurfaceHeight() > sample.surfaceHeight() + 0.05D) {
            return false;
        }
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double fringe = 5.0D + Math.min(12.0D, Math.sqrt(river.flow()) * 1.80D);
        return river.distance() <= halfWidth + fringe;
    }

    /**
     * Returns whether the riverbank sample belongs to the damp waterline portion of the envelope.
     *
     * @param sample Engine terrain sample
     * @return {@code true} in the inner bank transition
     */
    public static boolean isWetBank(TerrainSample sample) {
        if (!isRiverBank(sample)) {
            return false;
        }
        var river = sample.river();
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double wetReach = 2.5D + Math.min(4.5D, Math.sqrt(river.flow()) * 0.70D);
        return river.distance() <= halfWidth + wetReach;
    }

    /**
     * Returns whether the sample belongs to the outer, normally dry bank transition.
     *
     * @param sample Engine terrain sample
     * @return {@code true} in the outer riverbank fringe
     */
    public static boolean isOuterBank(TerrainSample sample) {
        return isRiverBank(sample) && !isWetBank(sample);
    }
}
