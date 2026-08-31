package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/** Shared predicates for the vegetated fringe around materialized rivers. */
public final class RiparianZone {

    private static final double MINIMUM_FRINGE = 4.0D;
    private static final double MAXIMUM_FLOW_FRINGE = 10.0D;
    private static final double FLOW_FRINGE_SCALE = 1.65D;

    private RiparianZone() {
    }

    /**
     * Returns whether the sample is dry-climate riverbank rather than the wet channel itself.
     *
     * @param sample Engine terrain sample
     * @return {@code true} when the sample belongs to the dry-climate riparian fringe
     */
    public static boolean isDryBank(TerrainSample sample) {
        if (StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType())) {
            return false;
        }
        RiverSample river = sample.river();
        if (!river.isAvailable() || !river.hasFlow() || !(river.flow() > 0.0D)) {
            return false;
        }
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double fringe = MINIMUM_FRINGE
                + Math.min(MAXIMUM_FLOW_FRINGE, Math.sqrt(river.flow()) * FLOW_FRINGE_SCALE);
        if (river.distance() > halfWidth + fringe) {
            return false;
        }
        if (!sample.climate().isAvailable()) {
            return false;
        }
        return sample.climate().temperature() > 0.62D
                && sample.climate().moisture() < 0.46D;
    }
}
