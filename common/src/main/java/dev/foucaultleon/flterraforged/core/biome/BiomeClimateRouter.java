package dev.foucaultleon.flterraforged.core.biome;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.Objects;

/** Selects version-neutral biome roles from continuous Engine climate and terrain semantics. */
public final class BiomeClimateRouter {

    private BiomeClimateRouter() {
    }

    /**
     * Resolves one semantic biome role.
     *
     * @param sample Engine terrain sample
     * @return version-neutral biome role
     */
    public static BiomeRole route(TerrainSample sample) {
        Objects.requireNonNull(sample, "sample");
        TerrainType terrain = sample.terrainType();
        ClimateSample climate = sample.climate();
        double temperature = climate.isAvailable() ? climate.temperature() : 0.5D;
        double moisture = climate.isAvailable() ? climate.moisture() : 0.5D;
        double slope = sample.hasSlope() ? sample.slope() : 0.0D;

        if (StandardTerrainTypes.OCEAN.equals(terrain)) {
            if (temperature < 0.28D) {
                return BiomeRole.OCEAN_COLD;
            }
            return temperature > 0.72D ? BiomeRole.OCEAN_WARM : BiomeRole.OCEAN_TEMPERATE;
        }
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return temperature < 0.25D ? BiomeRole.RIVER_COLD : BiomeRole.RIVER_TEMPERATE;
        }
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return slope > 0.85D ? BiomeRole.COAST_ROCKY : BiomeRole.COAST_SANDY;
        }
        if (isDryRiparianBank(sample)) {
            return temperature > 0.68D
                    ? BiomeRole.MEDITERRANEAN_GRASSLAND
                    : BiomeRole.TEMPERATE_GRASSLAND;
        }
        if (StandardTerrainTypes.MOUNTAINS.equals(terrain)) {
            if (temperature < 0.23D) {
                return BiomeRole.POLAR_PLAIN;
            }
            return moisture > 0.43D ? BiomeRole.ALPINE_MEADOW : BiomeRole.ALPINE_ROCK;
        }
        if (temperature < 0.16D) {
            return BiomeRole.POLAR_PLAIN;
        }
        if (temperature < 0.30D) {
            return moisture >= 0.40D ? BiomeRole.BOREAL_FOREST : BiomeRole.COOL_GRASSLAND;
        }
        if (temperature < 0.44D) {
            if (moisture >= 0.58D) {
                return BiomeRole.COOL_FOREST;
            }
            return moisture >= 0.43D ? BiomeRole.TEMPERATE_OPEN_WOODLAND : BiomeRole.COOL_GRASSLAND;
        }
        if (temperature <= 0.68D) {
            if (moisture > 0.84D && slope < 0.40D) {
                return BiomeRole.WETLAND;
            }
            if (moisture > 0.74D) {
                return BiomeRole.TEMPERATE_DENSE_FOREST;
            }
            if (moisture > 0.59D) {
                return BiomeRole.TEMPERATE_FOREST;
            }
            if (moisture > 0.46D) {
                return BiomeRole.TEMPERATE_OPEN_WOODLAND;
            }
            if ((StandardTerrainTypes.HILLS.equals(terrain)
                    || StandardTerrainTypes.PLATEAU.equals(terrain))
                    && moisture > 0.38D) {
                return BiomeRole.ALPINE_MEADOW;
            }
            return BiomeRole.TEMPERATE_GRASSLAND;
        }
        if (temperature <= 0.82D) {
            if (moisture > 0.52D) {
                return BiomeRole.MEDITERRANEAN_WOODLAND;
            }
            return BiomeRole.MEDITERRANEAN_GRASSLAND;
        }
        if (moisture < 0.18D) {
            return BiomeRole.HOT_DRY;
        }
        if (moisture < 0.58D) {
            return BiomeRole.HOT_SEASONAL;
        }
        return BiomeRole.HOT_WET;
    }

    /**
     * Computes the climate/terrain component of natural woodland density.
     *
     * <p>The Minecraft adapter combines this broad value with a slow deterministic spatial field.
     * This prevents an entire climate region from becoming one uniform tree-density class while
     * retaining gradual ecological transitions.</p>
     *
     * @param sample Engine terrain sample
     * @return normalized woodland-density propensity in {@code [0,1]}
     */
    public static double woodlandDensity(TerrainSample sample) {
        Objects.requireNonNull(sample, "sample");
        double moisture = sample.climate().isAvailable() ? sample.climate().moisture() : 0.5D;
        double slope = sample.hasSlope() ? Math.min(1.0D, sample.slope() / 1.8D) : 0.0D;
        double erosion = sample.hasErosion()
                ? clamp01(sample.erosion() * 0.5D + 0.5D)
                : 0.5D;
        double continental = sample.hasContinentalness()
                ? clamp01(sample.continentalness() * 0.5D + 0.5D)
                : 0.5D;
        return clamp01(
                moisture * 0.58D
                        + (1.0D - slope) * 0.18D
                        + erosion * 0.12D
                        + continental * 0.12D);
    }

    /**
     * Returns whether this point is the dry-climate fringe beside a river channel.
     *
     * @param sample Engine terrain sample
     * @return {@code true} for a dry riparian bank
     */
    public static boolean isDryRiparianBank(TerrainSample sample) {
        if (StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType())
                || !sample.climate().isAvailable()) {
            return false;
        }
        var river = sample.river();
        if (!river.isAvailable() || !river.hasFlow() || !(river.flow() > 0.0D)) {
            return false;
        }
        if (river.hasWaterSurfaceHeight()
                && river.waterSurfaceHeight() > sample.surfaceHeight() + 0.05D) {
            return false;
        }
        double temperature = sample.climate().temperature();
        double moisture = sample.climate().moisture();
        if (temperature <= 0.58D || moisture < 0.30D || moisture >= 0.52D) {
            return false;
        }
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double fringe = 3.0D + Math.min(6.0D, Math.sqrt(river.flow()));
        return river.distance() <= halfWidth + fringe;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
