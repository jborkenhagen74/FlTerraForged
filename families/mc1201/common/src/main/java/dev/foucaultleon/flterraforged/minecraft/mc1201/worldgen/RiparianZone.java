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
        return bankStrength(sample) > 0.0D;
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
        return bankStrength(sample) >= 0.58D;
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

    /**
     * Returns continuous riverbank influence from one at the waterline to zero in the biome.
     *
     * <p>R58 keeps the structural bank substantially narrower than R57. Dry climates narrow it
     * again so river crossings through sandy terrain do not create broad green ribbons. The
     * transition still scales weakly with accumulated flow, preserving wider banks on major rivers
     * without allowing the envelope to dominate a whole local biome.</p>
     *
     * @param sample Engine terrain sample
     * @return bank influence in {@code [0,1]}
     */
    public static double bankStrength(TerrainSample sample) {
        var river = sample.river();
        if (!river.isAvailable() || !river.hasFlow() || !(river.flow() > 0.0D)) {
            return 0.0D;
        }
        if (river.hasWaterSurfaceHeight()
                && river.waterSurfaceHeight() > sample.surfaceHeight() + 0.05D) {
            return 0.0D;
        }
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double fringe = 6.0D + Math.min(4.0D, Math.sqrt(river.flow()) * 0.65D);
        if (sample.climate().isAvailable()
                && sample.climate().temperature() > 0.58D
                && sample.climate().moisture() < 0.52D) {
            fringe *= 0.55D;
        }
        double offset = Math.max(0.0D, river.distance() - halfWidth);
        return 1.0D - smooth(Math.min(1.0D, offset / fringe));
    }

    /**
     * Returns continuous dry-lake-shore influence encoded by the Engine's signed distance.
     *
     * @param sample Engine terrain sample
     * @return shore influence in {@code [0,1]}
     */
    public static double lakeShoreStrength(TerrainSample sample) {
        var river = sample.river();
        if (!river.isAvailable() || river.hasFlow() && river.flow() > 0.0D) {
            return 0.0D;
        }
        double transition = Math.max(1.0D, river.width());
        double outwardDistance = Math.max(0.0D, river.distance());
        return 1.0D - smooth(Math.min(1.0D, outwardDistance / transition));
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }
}
