package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.EngineContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void rejectsAnInlandCenterAfterOneEngineSample() {
        AtomicInteger samples = new AtomicInteger();
        TerrainWorld inland = countingWorld(samples, (x, z) -> sample(
                80.0D,
                StandardTerrainTypes.MOUNTAINS));

        assertFalse(MarineStructureGuard.permits(
                "minecraft:shipwreck", 8, 8, 63, inland));
        assertEquals(1, samples.get(), "inland rejection must not scan the perimeter");
    }

    @Test
    void deepMarineAcceptanceUsesOneCenterAndEightPerimeterSamples() {
        AtomicInteger samples = new AtomicInteger();
        TerrainWorld ocean = countingWorld(samples, (x, z) -> sample(
                48.0D,
                StandardTerrainTypes.OCEAN));

        assertTrue(MarineStructureGuard.permits(
                "minecraft:monument", 8, 8, 63, ocean));
        assertEquals(9, samples.get(), "accepted marine start must use the bounded nine-point survey");
    }

    @Test
    void emptyAndNonMarineStartsDoNotSampleTheEngine() {
        AtomicInteger samples = new AtomicInteger();
        TerrainWorld ocean = countingWorld(samples, (x, z) -> sample(
                48.0D,
                StandardTerrainTypes.OCEAN));

        assertTrue(MarineStructureGuard.permits(
                "minecraft:shipwreck", false, 8, 8, 63, ocean));
        assertTrue(MarineStructureGuard.permits(
                "minecraft:village_plains", true, 8, 8, 63, ocean));
        assertEquals(0, samples.get(), "irrelevant starts must not query terrain");
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

    private static TerrainWorld countingWorld(
            AtomicInteger count,
            BiFunction<Integer, Integer, TerrainSample> samples) {
        return new TerrainWorld() {
            @Override
            public EngineContext context() {
                return new EngineContext(1L, -64, 320, 63);
            }

            @Override
            public TerrainSample sample(int x, int z) {
                count.incrementAndGet();
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
