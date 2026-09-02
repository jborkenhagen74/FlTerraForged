package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Map;
import java.util.Objects;

/** Prevents marine structures from starting in inland or undersized water bodies. */
final class MarineStructureGuard {

    private static final int SAMPLE_RADIUS = 32;
    private static final int SAMPLE_STEP = 16;
    private static final double EDGE_MINIMUM_DEPTH = 2.0D;
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
     * five-by-five connected marine sample field around the start chunk. Every sample must be
     * actual ocean/coast terrain with at least two blocks of water, while the center additionally
     * has to satisfy the structure-specific depth. A nearby ocean biome can therefore no longer
     * authorize a shipwreck inside a river, lake or puddle.</p>
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
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(world, "world");
        Double centerMinimumDepth = MINIMUM_CENTER_DEPTHS.get(structureId);
        if (centerMinimumDepth == null) {
            return true;
        }

        for (int offsetX = -SAMPLE_RADIUS; offsetX <= SAMPLE_RADIUS; offsetX += SAMPLE_STEP) {
            for (int offsetZ = -SAMPLE_RADIUS; offsetZ <= SAMPLE_RADIUS; offsetZ += SAMPLE_STEP) {
                TerrainSample sample = world.sample(centerX + offsetX, centerZ + offsetZ);
                if (!isMarine(sample)
                        || seaLevel - sample.surfaceHeight() < EDGE_MINIMUM_DEPTH) {
                    return false;
                }
            }
        }

        TerrainSample center = world.sample(centerX, centerZ);
        return seaLevel - center.surfaceHeight() >= centerMinimumDepth;
    }

    private static boolean isMarine(TerrainSample sample) {
        return StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType());
    }
}
