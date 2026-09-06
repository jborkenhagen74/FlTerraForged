package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.core.biome.BiomeRole;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

/** Maps version-neutral climate roles to the native Minecraft 1.20.1 biome palette. */
public final class NativeBiomeRouter {

    private static final int WOODLAND_VARIATION_SCALE = 192;

    private NativeBiomeRouter() {
    }

    /**
     * Resolves a Minecraft biome using world-position-aware woodland variation.
     *
     * @param sample external-engine terrain sample
     * @param palette configured native biome palette
     * @param seaLevel active Minecraft sea level
     * @param blockX world block X
     * @param blockZ world block Z
     * @param seed world seed
     * @return selected Minecraft biome entry
     */
    public static RegistryEntry<Biome> route(
            TerrainSample sample,
            BiomePalette palette,
            int seaLevel,
            int blockX,
            int blockZ,
            long seed) {
        BiomeRole role = BiomeClimateRouter.route(sample);
        role = applyWoodlandDensity(role, sample, blockX, blockZ, seed);
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                && seaLevel - sample.surfaceHeight() >= 12.0D) {
            role = switch (role) {
                case OCEAN_COLD -> BiomeRole.OCEAN_DEEP_COLD;
                case OCEAN_WARM -> BiomeRole.OCEAN_DEEP_WARM;
                default -> BiomeRole.OCEAN_DEEP_TEMPERATE;
            };
        }
        return palette.resolve(role, sample, blockX, blockZ, seed);
    }

    /**
     * Compatibility route for callers without world coordinates.
     *
     * @param sample external-engine terrain sample
     * @param palette configured native biome palette
     * @param seaLevel active Minecraft sea level
     * @return selected Minecraft biome entry
     */
    public static RegistryEntry<Biome> route(
            TerrainSample sample,
            BiomePalette palette,
            int seaLevel) {
        return route(sample, palette, seaLevel, 0, 0, 0L);
    }

    private static BiomeRole applyWoodlandDensity(
            BiomeRole role,
            TerrainSample sample,
            int blockX,
            int blockZ,
            long seed) {
        double ecological = BiomeClimateRouter.woodlandDensity(sample);
        double spatial = macroVariation(blockX, blockZ, seed);
        double density = ecological * 0.72D + spatial * 0.28D;
        return switch (role) {
            case BOREAL_FOREST -> density < 0.38D ? BiomeRole.COOL_GRASSLAND : role;
            case COOL_FOREST -> density < 0.34D
                    ? BiomeRole.COOL_GRASSLAND
                    : density < 0.55D ? BiomeRole.TEMPERATE_OPEN_WOODLAND : role;
            case TEMPERATE_OPEN_WOODLAND -> density < 0.28D
                    ? BiomeRole.TEMPERATE_GRASSLAND
                    : role;
            case TEMPERATE_FOREST -> density < 0.32D
                    ? BiomeRole.TEMPERATE_GRASSLAND
                    : density < 0.52D
                            ? BiomeRole.TEMPERATE_OPEN_WOODLAND
                            : density > 0.82D ? BiomeRole.TEMPERATE_DENSE_FOREST : role;
            case TEMPERATE_DENSE_FOREST -> density < 0.58D
                    ? BiomeRole.TEMPERATE_FOREST
                    : role;
            case MEDITERRANEAN_WOODLAND -> density < 0.42D
                    ? BiomeRole.MEDITERRANEAN_GRASSLAND
                    : role;
            default -> role;
        };
    }

    private static double macroVariation(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, WOODLAND_VARIATION_SCALE);
        int cellZ = Math.floorDiv(z, WOODLAND_VARIATION_SCALE);
        double tx = smooth(Math.floorMod(x, WOODLAND_VARIATION_SCALE)
                / (double) WOODLAND_VARIATION_SCALE);
        double tz = smooth(Math.floorMod(z, WOODLAND_VARIATION_SCALE)
                / (double) WOODLAND_VARIATION_SCALE);
        double a = hash01(cellX, cellZ, seed);
        double b = hash01(cellX + 1, cellZ, seed);
        double c = hash01(cellX, cellZ + 1, seed);
        double d = hash01(cellX + 1, cellZ + 1, seed);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private static double hash01(int x, int z, long seed) {
        long value = seed ^ 0xA0761D6478BD642FL;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }
}
