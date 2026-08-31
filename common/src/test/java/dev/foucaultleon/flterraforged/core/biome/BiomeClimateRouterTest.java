package dev.foucaultleon.flterraforged.core.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import org.junit.jupiter.api.Test;

final class BiomeClimateRouterTest {

    @Test
    void temperateMoistureProgressesThroughPlausibleRoles() {
        assertEquals(BiomeRole.TEMPERATE_GRASSLAND, role(0.55D, 0.30D));
        assertEquals(BiomeRole.TEMPERATE_OPEN_WOODLAND, role(0.55D, 0.52D));
        assertEquals(BiomeRole.TEMPERATE_FOREST, role(0.55D, 0.66D));
        assertEquals(BiomeRole.TEMPERATE_DENSE_FOREST, role(0.55D, 0.79D));
        assertEquals(BiomeRole.WETLAND, role(0.55D, 0.90D));
    }

    @Test
    void warmDryClimateUsesTransitionRolesBeforeDesert() {
        assertEquals(BiomeRole.MEDITERRANEAN_GRASSLAND, role(0.76D, 0.34D));
        assertEquals(BiomeRole.HOT_SEASONAL, role(0.88D, 0.34D));
        assertEquals(BiomeRole.HOT_DRY, role(0.88D, 0.10D));
    }

    private static BiomeRole role(double temperature, double moisture) {
        TerrainSample sample = new TerrainSample(
                80.0D,
                0.1D,
                0.0D,
                0.2D,
                StandardTerrainTypes.PLAINS,
                new ClimateSample(temperature, moisture),
                RiverSample.UNAVAILABLE);
        return BiomeClimateRouter.route(sample);
    }
}
