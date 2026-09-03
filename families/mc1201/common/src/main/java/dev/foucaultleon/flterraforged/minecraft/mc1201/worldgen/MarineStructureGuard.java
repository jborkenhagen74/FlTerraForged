package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.MarineEnvironmentCache.MarineColumn;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.MarineEnvironmentCache.MarineEnvironmentSummary;
import java.util.Objects;

/** Prevents marine structures from starting in inland, undersized or physically shallow water. */
final class MarineStructureGuard {

    /**
     * R38 runtime-control switch. The guard is deliberately disabled so this revision can isolate
     * whether early structure-environment sampling is responsible for the Minecraft 0% worldgen
     * stall. Do not promote this control setting to a release revision.
     */
    private static final boolean ENABLED = false;
    private static final double EDGE_MINIMUM_DEPTH = 2.0D;
    private static final double MONUMENT_EDGE_MINIMUM_DEPTH = 4.0D;

    private MarineStructureGuard() {
    }

    /**
     * Tests a vanilla structure start against the materialized FlTerraForged environment.
     *
     * <p>R38 is an explicit runtime-control revision. While {@link #ENABLED} is {@code false}, all
     * starts are retained before any Engine terrain sample is requested. This isolates the early
     * structure-start sampling path from all other R37/R33 world-generation behavior.</p>
     *
     * @param structureId namespaced Minecraft structure identifier
     * @param hasChildren whether vanilla created at least one structure piece
     * @param centerX block X at the center of the candidate start chunk
     * @param centerZ block Z at the center of the candidate start chunk
     * @param world bound Engine terrain sampler
     * @param cache world-generator scoped environment cache
     * @return {@code true} when vanilla may retain the structure start
     */
    static boolean permits(
            String structureId,
            boolean hasChildren,
            int centerX,
            int centerZ,
            TerrainWorld world,
            MarineEnvironmentCache cache) {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(cache, "cache");
        if (!ENABLED) {
            return true;
        }
        if (!hasChildren) {
            return true;
        }

        MarineRule rule = MarineRule.forStructure(structureId);
        if (rule == MarineRule.NONE) {
            return true;
        }

        MarineColumn center = cache.column(world, centerX, centerZ);
        if (rule == MarineRule.BEACHED_SHIPWRECK) {
            if (!isPlausibleBeachedCenter(center)) {
                return false;
            }
            return permitsBeached(cache.summary(world, centerX, centerZ));
        }
        if (!center.isMarineWater()
                || center.inlandWater()
                || center.waterDepth() < rule.minimumCenterDepth) {
            return false;
        }

        MarineEnvironmentSummary summary = cache.summary(world, centerX, centerZ);
        if (summary.inner().inlandWater() > 0 || summary.outer().inlandWater() > 0) {
            return false;
        }
        if (summary.inner().marineWater() < rule.minimumInnerMarineSamples
                || summary.outer().marineWater() < rule.minimumOuterMarineSamples) {
            return false;
        }
        if (summary.inner().minimumMarineDepth() < rule.minimumEdgeDepth) {
            return false;
        }
        return summary.outer().marineWater() == 0
                || summary.outer().minimumMarineDepth() >= EDGE_MINIMUM_DEPTH;
    }

    private static boolean isPlausibleBeachedCenter(MarineColumn center) {
        if (center.inlandWater() || !center.geometry().supportsDryPlacement()) {
            return false;
        }
        return center.coast() || !center.materializedWater();
    }

    private static boolean permitsBeached(MarineEnvironmentSummary summary) {
        if (summary.inner().inlandWater() > 0 || summary.outer().inlandWater() > 0) {
            return false;
        }
        return summary.inner().marineWater() >= 2
                && summary.outer().marineWater() >= 1;
    }

    private enum MarineRule {
        NONE(0.0D, 0.0D, 0, 0),
        SHIPWRECK(5.0D, EDGE_MINIMUM_DEPTH, 7, 3),
        OCEAN_RUIN(6.0D, EDGE_MINIMUM_DEPTH, 8, 3),
        MONUMENT(12.0D, MONUMENT_EDGE_MINIMUM_DEPTH, 8, 4),
        OCEAN_PORTAL(5.0D, EDGE_MINIMUM_DEPTH, 7, 3),
        BEACHED_SHIPWRECK(0.0D, 0.0D, 0, 0);

        private final double minimumCenterDepth;
        private final double minimumEdgeDepth;
        private final int minimumInnerMarineSamples;
        private final int minimumOuterMarineSamples;

        MarineRule(
                double minimumCenterDepth,
                double minimumEdgeDepth,
                int minimumInnerMarineSamples,
                int minimumOuterMarineSamples) {
            this.minimumCenterDepth = minimumCenterDepth;
            this.minimumEdgeDepth = minimumEdgeDepth;
            this.minimumInnerMarineSamples = minimumInnerMarineSamples;
            this.minimumOuterMarineSamples = minimumOuterMarineSamples;
        }

        static MarineRule forStructure(String structureId) {
            return switch (structureId) {
                case "minecraft:shipwreck" -> SHIPWRECK;
                case "minecraft:shipwreck_beached" -> BEACHED_SHIPWRECK;
                case "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm" -> OCEAN_RUIN;
                case "minecraft:monument" -> MONUMENT;
                case "minecraft:ruined_portal_ocean" -> OCEAN_PORTAL;
                default -> NONE;
            };
        }
    }
}
