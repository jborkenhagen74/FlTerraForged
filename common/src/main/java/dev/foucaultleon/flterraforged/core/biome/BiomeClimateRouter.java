package dev.foucaultleon.flterraforged.core.biome;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.Objects;

/**
 * Selects version-neutral biome roles from continuous Engine climate and terrain semantics.
 *
 * <p>The thresholds deliberately contain intermediate roles between climate extremes. With the
 * Engine's continuous climate fields this means a hot/dry region transitions through warm dry
 * grassland/woodland before reaching temperate forest or wetland instead of allowing implausible
 * direct desert-to-swamp or desert-to-dense-forest boundaries.</p>
 */
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
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return slope > 0.85D ? BiomeRole.COAST_ROCKY : BiomeRole.COAST_SANDY;
        }
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return temperature < 0.25D ? BiomeRole.RIVER_COLD : BiomeRole.RIVER_TEMPERATE;
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
            // Warm-temperate dry regions remain Mediterranean in character. Savanna/jungle
            // semantics are reserved for genuinely hot macroclimates so a temperate preset does
            // not produce tropical islands at a regional moisture minimum.
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
     * Returns whether this point is the dry-climate fringe beside a river channel.
     *
     * @param sample Engine terrain sample
     * @return {@code true} for a dry riparian bank
     */
    public static boolean isDryRiparianBank(TerrainSample sample) {
        if (StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType())) {
            return false;
        }
        var river = sample.river();
        if (!river.isAvailable() || !river.hasFlow() || !(river.flow() > 0.0D)) {
            return false;
        }
        double halfWidth = Math.max(1.0D, river.width() * 0.5D);
        double fringe = 4.0D + Math.min(10.0D, Math.sqrt(river.flow()) * 1.65D);
        if (river.distance() > halfWidth + fringe || !sample.climate().isAvailable()) {
            return false;
        }
        return sample.climate().temperature() > 0.62D
                && sample.climate().moisture() < 0.46D;
    }
}
