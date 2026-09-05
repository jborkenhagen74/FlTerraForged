package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.MarineEnvironmentCache.MarineColumn;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.MarineEnvironmentCache.OpenWaterProfile;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.MarineEnvironmentCache.RingStats;
import java.util.Objects;

/** Prevents marine structures from starting in inland, confined or physically shallow water. */
final class MarineStructureGuard {

    private static final double EDGE_MINIMUM_DEPTH = 2.0D;
    private static final double MONUMENT_EDGE_MINIMUM_DEPTH = 4.0D;

    private MarineStructureGuard() {
    }

    /**
     * Returns whether the structure start needs a FlTerraForged environment decision.
     *
     * <p>The check is deliberately pure and must run before any Engine world is bound. Empty starts
     * and unrelated structures therefore keep the fast path and never initialize placement-time
     * Engine state.</p>
     *
     * @param structureId namespaced Minecraft structure identifier
     * @param hasChildren whether vanilla created at least one structure piece
     * @return {@code true} when an environment probe is required
     */
    static boolean requiresEnvironment(String structureId, boolean hasChildren) {
        Objects.requireNonNull(structureId, "structureId");
        return hasChildren && MarineRule.forStructure(structureId) != MarineRule.NONE;
    }

    /**
     * Tests a vanilla structure start against the materialized FlTerraForged environment.
     *
     * <p>R53 adds a provider-resolved open-water stage between the center fast rejection and the
     * existing large structure rings. A candidate whose Engine center happens to carry OCEAN
     * semantics is still rejected when the surrounding physical water forms a narrow river mouth,
     * lake outlet, small depression or other confined channel. Every profile sample reuses the same
     * single-flight column cache and therefore observes the selected provider's real partial-block
     * and waterlogging geometry.</p>
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
            OpenWaterProfile profile = cache.waterBodyProfile(world, centerX, centerZ);
            if (!profile.openMarineAccess()) {
                return false;
            }
            RingStats inner = cache.innerRing(world, centerX, centerZ);
            return inner.inlandWater() == 0 && inner.oceanWater() >= 1;
        }

        // Underwater structures require actual OCEAN semantics at the center and physical water.
        // The open-water profile then proves that this is broad marine water rather than an ocean-
        // classified trench or a narrow receiver corridor below sea level.
        if (!center.ocean()
                || !center.materializedWater()
                || center.inlandWater()
                || center.waterDepth() < rule.minimumCenterDepth) {
            return false;
        }
        OpenWaterProfile profile = cache.waterBodyProfile(world, centerX, centerZ);
        if (!profile.isOpenMarine()) {
            return false;
        }

        RingStats inner = cache.innerRing(world, centerX, centerZ);
        if (!permitsRing(inner, rule.minimumInnerOceanSamples, rule.minimumEdgeDepth)) {
            return false;
        }
        if (!rule.requiresOuterRing) {
            return true;
        }

        RingStats outer = cache.outerRing(world, centerX, centerZ);
        return permitsRing(outer, rule.minimumOuterOceanSamples, MONUMENT_EDGE_MINIMUM_DEPTH);
    }

    private static boolean permitsRing(
            RingStats ring,
            int minimumOceanSamples,
            double minimumDepth) {
        if (ring.inlandWater() > 0 || ring.oceanWater() < minimumOceanSamples) {
            return false;
        }
        return ring.marineWater() == 0 || ring.minimumMarineDepth() >= minimumDepth;
    }

    private static boolean isPlausibleBeachedCenter(MarineColumn center) {
        if (center.inlandWater() || !center.geometry().supportsDryPlacement()) {
            return false;
        }
        return center.coast() && !center.materializedWater();
    }

    private enum MarineRule {
        NONE(0.0D, 0.0D, 0, 0, false),
        SHIPWRECK(5.0D, EDGE_MINIMUM_DEPTH, 3, 0, false),
        OCEAN_RUIN(6.0D, EDGE_MINIMUM_DEPTH, 4, 0, false),
        MONUMENT(12.0D, MONUMENT_EDGE_MINIMUM_DEPTH, 4, 4, true),
        OCEAN_PORTAL(5.0D, EDGE_MINIMUM_DEPTH, 3, 0, false),
        BEACHED_SHIPWRECK(0.0D, 0.0D, 0, 0, false);

        private final double minimumCenterDepth;
        private final double minimumEdgeDepth;
        private final int minimumInnerOceanSamples;
        private final int minimumOuterOceanSamples;
        private final boolean requiresOuterRing;

        MarineRule(
                double minimumCenterDepth,
                double minimumEdgeDepth,
                int minimumInnerOceanSamples,
                int minimumOuterOceanSamples,
                boolean requiresOuterRing) {
            this.minimumCenterDepth = minimumCenterDepth;
            this.minimumEdgeDepth = minimumEdgeDepth;
            this.minimumInnerOceanSamples = minimumInnerOceanSamples;
            this.minimumOuterOceanSamples = minimumOuterOceanSamples;
            this.requiresOuterRing = requiresOuterRing;
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
