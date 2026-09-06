package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;

/** Keeps broad terrestrial structures away from oceans and unstable shorelines before NOISE. */
final class TerrestrialStructureGuard {

    private static final int VILLAGE_RADIUS = 48;
    private static final int VILLAGE_STEP = 8;
    private static final double MINIMUM_DRY_RATIO = 0.92D;
    private static final double MINIMUM_CENTER_CLEARANCE = 2.0D;
    private static final double MAXIMUM_LOCAL_RELIEF = 18.0D;

    private TerrestrialStructureGuard() {
    }

    /**
     * Tests coarse terrestrial structure suitability without forcing exact terrain generation.
     *
     * <p>Village structure starts happen before the Engine-owned NOISE status. Calling
     * {@link TerrainWorld#sample(int, int)} here would recreate the old cold-start problem, so R62
     * deliberately uses only {@link TerrainWorld#placementSample(int, int)}. The test is conservative:
     * a village needs a dry center, a large dry footprint and bounded local relief. COAST and OCEAN
     * samples are treated as unsafe because village pieces extend well beyond the start chunk.</p>
     *
     * @param structureId namespaced structure identifier
     * @param hasChildren whether Minecraft created structure pieces
     * @param centerX candidate start-chunk center X
     * @param centerZ candidate start-chunk center Z
     * @param seaLevel global sea level
     * @param world bound Engine terrain sampler
     * @return {@code true} if the structure may be retained
     */
    static boolean permits(
            String structureId,
            boolean hasChildren,
            int centerX,
            int centerZ,
            int seaLevel,
            TerrainWorld world) {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(world, "world");
        if (!hasChildren || !isVillage(structureId)) {
            return true;
        }

        TerrainSample center = world.placementSample(centerX, centerZ);
        if (!isStableDryLand(center, seaLevel, MINIMUM_CENTER_CLEARANCE)) {
            return false;
        }

        int total = 0;
        int dry = 0;
        double minimumHeight = Double.POSITIVE_INFINITY;
        double maximumHeight = Double.NEGATIVE_INFINITY;
        for (int dz = -VILLAGE_RADIUS; dz <= VILLAGE_RADIUS; dz += VILLAGE_STEP) {
            for (int dx = -VILLAGE_RADIUS; dx <= VILLAGE_RADIUS; dx += VILLAGE_STEP) {
                TerrainSample sample = world.placementSample(centerX + dx, centerZ + dz);
                total++;
                if (isStableDryLand(sample, seaLevel, 0.5D)) {
                    dry++;
                    minimumHeight = Math.min(minimumHeight, sample.surfaceHeight());
                    maximumHeight = Math.max(maximumHeight, sample.surfaceHeight());
                }
            }
        }

        if (dry < Math.ceil(total * MINIMUM_DRY_RATIO)) {
            return false;
        }
        return maximumHeight - minimumHeight <= MAXIMUM_LOCAL_RELIEF;
    }

    private static boolean isVillage(String structureId) {
        return structureId.startsWith("minecraft:village_");
    }

    private static boolean isStableDryLand(
            TerrainSample sample,
            int seaLevel,
            double clearance) {
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType())
                || StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())) {
            return false;
        }
        return sample.surfaceHeight() >= seaLevel + clearance;
    }
}
