package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import org.junit.jupiter.api.Test;

final class TerrainWorldEnvironmentTest {

    @Test
    void defaultEnvironmentFallbackPreservesMarineWaterSurface() {
        TerrainWorld world = fixedWorld(new TerrainSample(
                52.0D,
                0.1D,
                0.0D,
                -0.8D,
                StandardTerrainTypes.OCEAN,
                ClimateSample.UNAVAILABLE,
                RiverSample.UNAVAILABLE));

        TerrainEnvironmentSample environment = world.environment(4, 7);

        assertEquals(StandardTerrainTypes.OCEAN, environment.terrainType());
        assertEquals(52.0D, environment.surfaceHeight());
        assertEquals(63.0D, environment.waterSurfaceHeight());
        assertTrue(environment.hasWaterSurfaceHeight());
    }

    private static TerrainWorld fixedWorld(TerrainSample sample) {
        return new TerrainWorld() {
            @Override
            public EngineContext context() {
                return new EngineContext(1L, -64, 320, 63);
            }

            @Override
            public TerrainSample sample(int x, int z) {
                return sample;
            }
        };
    }
}
