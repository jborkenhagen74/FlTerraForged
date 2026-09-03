package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import java.util.Map;
import java.util.Objects;

/** Prevents marine structures from starting in inland or undersized water bodies. */
final class MarineStructureGuard {

    private static final int SAMPLE_RADIUS = 32;
    private static final double EDGE_MINIMUM_DEPTH = 2.0D;
    private static final int[][] PERIMETER_OFFSETS = {
        {-SAMPLE_RADIUS, -SAMPLE_RADIUS},
        {0, -SAMPLE_RADIUS},
        {SAMPLE_RADIUS, -SAMPLE_RADIUS},
        {-SAMPLE_RADIUS, 0},
        {SAMPLE_RADIUS, 0},
        {-SAMPLE_RADIUS, SAMPLE_RADIUS},
        {0, SAMPLE_RADIUS},
        {SAMPLE_RADIUS, SAMPLE_RADIUS}
    };
    private static final Map<String, Double> MINIMUM_CENTER_DEPTHS = Map.of(
            "minecraft:shipwreck", 5.0D,
            "minecraft:ocean_ruin_cold", 6.0D,
            "minecraft:ocean_ruin_warm", 6.0D,
            "minecraft:monument", 12.0D);

    private MarineStructureGuard() {
    }

    /**
     * Tests whether a newly selected structure start is compatible with Engine hydrology.
     *
     * <p>Non-marine structures are deliberately left to vanilla. Marine structures require a
     * bounded center-and-perimeter marine sample field around the start chunk. The center is
     * checked first, so the common inland rejection path performs exactly one lightweight marine
     * depth query.
     * Every perimeter point must be actual ocean/coast terrain with at least two blocks of water,
     * while the center additionally has to satisfy the structure-specific depth. A nearby ocean
     * biome can therefore no longer authorize a shipwreck inside a river, lake or puddle without
     * forcing a cold 5-by-5 terrain-tile scan or full hydrology generation on chunk workers.</p>
     *
     * @param structureId namespaced Minecraft structure identifier
     * @param centerX block X at the center of the candidate start chunk
     * @param centerZ block Z at the center of the candidate start chunk
     * @param seaLevel active world sea level
     * @param world bound Engine terrain sampler
     * @return {@code true} when vanilla may retain the structure start
     */
    static boolean permits(
            String structureId,
            int centerX,
            int centerZ,
            int seaLevel,
            TerrainWorld world) {
        return permits(structureId, true, centerX, centerZ, seaLevel, world);
    }

    /**
     * Tests a potentially empty structure start without sampling terrain unnecessarily.
     *
     * @param structureId namespaced Minecraft structure identifier
     * @param hasChildren whether vanilla created at least one structure piece
     * @param centerX block X at the center of the candidate start chunk
     * @param centerZ block Z at the center of the candidate start chunk
     * @param seaLevel active world sea level
     * @param world bound Engine terrain sampler
     * @return {@code true} when an empty, non-marine or valid marine start may be retained
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
        if (seaLevel != world.context().seaLevel()) {
            throw new IllegalArgumentException("structure and Engine sea levels must match");
        }
        Double centerMinimumDepth = MINIMUM_CENTER_DEPTHS.get(structureId);
        if (!hasChildren || centerMinimumDepth == null) {
            return true;
        }

        if (!world.isMarine(centerX, centerZ, centerMinimumDepth)) {
            return false;
        }

        for (int[] offset : PERIMETER_OFFSETS) {
            if (!world.isMarine(
                    centerX + offset[0],
                    centerZ + offset[1],
                    EDGE_MINIMUM_DEPTH)) {
                return false;
            }
        }
        return true;
    }
}
