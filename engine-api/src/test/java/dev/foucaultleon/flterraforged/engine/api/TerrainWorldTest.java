package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import org.junit.jupiter.api.Test;

final class TerrainWorldTest {

    @Test
    void defaultMarineQueryRemainsCompatibleWithExistingProviders() {
        TerrainWorld world = world(sample(56.5D, StandardTerrainTypes.OCEAN));
        assertTrue(world.isMarine(0, 0, 6.0D));
        assertFalse(world.isMarine(0, 0, 7.0D));
    }

    @Test
    void defaultMarineQueryRejectsInlandAndInvalidDepth() {
        TerrainWorld world = world(sample(40.0D, StandardTerrainTypes.LAKE));
        assertFalse(world.isMarine(0, 0, 5.0D));
        assertThrows(IllegalArgumentException.class, () -> world.isMarine(0, 0, -1.0D));
    }

    private static TerrainWorld world(TerrainSample sample) {
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

    private static TerrainSample sample(double height, TerrainType type) {
        return new TerrainSample(
                height,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                type,
                ClimateSample.UNAVAILABLE,
                RiverSample.UNAVAILABLE);
    }
}
