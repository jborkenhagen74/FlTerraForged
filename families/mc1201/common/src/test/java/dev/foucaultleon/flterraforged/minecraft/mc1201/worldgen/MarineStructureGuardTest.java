package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.EngineContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

final class MarineStructureGuardTest {

    @Test
    void acceptsShipwreckInBroadDeepOcean() {
        assertTrue(MarineStructureGuard.permits(
                "minecraft:shipwreck", 8, 8, 63, world((x, z) -> sample(52.0D,
                        StandardTerrainTypes.OCEAN))));
    }

    @Test
    void rejectsShipwreckInRiverLakeAndPuddle() {
        assertFalse(MarineStructureGuard.permits(
                "minecraft:shipwreck", 8, 8, 63, world((x, z) -> sample(52.0D,
                        StandardTerrainTypes.RIVER))));
        assertFalse(MarineStructureGuard.permits(
                "minecraft:shipwreck", 8, 8, 63, world((x, z) -> sample(52.0D,
                        StandardTerrainTypes.LAKE))));
        assertFalse(MarineStructureGuard.permits(
                "minecraft:shipwreck", 8, 8, 63, world((x, z) -> sample(61.5D,
                        StandardTerrainTypes.OCEAN))));
    }

    @Test
    void rejectsNearbyOceanThatDoesNotCoverTheCandidateFootprint() {
        TerrainWorld mixed = world((x, z) -> x < 24
                ? sample(52.0D, StandardTerrainTypes.OCEAN)
                : sample(58.0D, StandardTerrainTypes.RIVER));
        assertFalse(MarineStructureGuard.permits(
                "minecraft:ocean_ruin_cold", 8, 8, 63, mixed));
    }

    @Test
    void monumentRequiresDeepMarineCenter() {
        assertFalse(MarineStructureGuard.permits(
                "minecraft:monument", 8, 8, 63, world((x, z) -> sample(53.0D,
                        StandardTerrainTypes.OCEAN))));
        assertTrue(MarineStructureGuard.permits(
                "minecraft:monument", 8, 8, 63, world((x, z) -> sample(49.0D,
                        StandardTerrainTypes.OCEAN))));
    }

    @Test
    void doesNotChangeNonMarineStructures() {
        assertTrue(MarineStructureGuard.permits(
                "minecraft:village_plains", 8, 8, 63, world((x, z) -> sample(80.0D,
                        StandardTerrainTypes.MOUNTAINS))));
    }

    private static TerrainWorld world(BiFunction<Integer, Integer, TerrainSample> samples) {
        return new TerrainWorld() {
            @Override
            public EngineContext context() {
                return new EngineContext(1L, -64, 320, 63);
            }

            @Override
            public TerrainSample sample(int x, int z) {
                return samples.apply(x, z);
            }
        };
    }

    private static TerrainSample sample(double height, TerrainType type) {
        return new TerrainSample(
                height,
                0.0D,
                0.0D,
                0.0D,
                type,
                ClimateSample.UNAVAILABLE,
                RiverSample.UNAVAILABLE);
    }
}
