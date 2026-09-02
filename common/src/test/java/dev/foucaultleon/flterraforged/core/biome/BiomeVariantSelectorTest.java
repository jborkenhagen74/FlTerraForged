package dev.foucaultleon.flterraforged.core.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BiomeVariantSelectorTest {

    @Test
    void seededForestStandsAreDeterministicAndUseEveryCandidate() {
        int[] counts = new int[4];
        long seed = 123456789L;
        for (int z = -8192; z < 8192; z += 64) {
            for (int x = -8192; x < 8192; x += 64) {
                int first = BiomeVariantSelector.index(
                        BiomeRole.TEMPERATE_FOREST, counts.length, x, z, seed);
                int second = BiomeVariantSelector.index(
                        BiomeRole.TEMPERATE_FOREST, counts.length, x, z, seed);
                assertEquals(first, second);
                counts[first]++;
            }
        }
        int samples = java.util.Arrays.stream(counts).sum();
        for (int count : counts) {
            double share = count / (double) samples;
            assertTrue(share > 0.18D, "every configured forest candidate must remain represented");
            assertTrue(share < 0.32D, "one forest candidate must not dominate the spatial mosaic");
        }
    }

    @Test
    void changingTheWorldSeedChangesForestStandOwnership() {
        int differences = 0;
        for (int z = -2048; z <= 2048; z += 128) {
            for (int x = -2048; x <= 2048; x += 128) {
                int first = BiomeVariantSelector.index(
                        BiomeRole.COOL_FOREST, 4, x, z, 123L);
                int second = BiomeVariantSelector.index(
                        BiomeRole.COOL_FOREST, 4, x, z, 456L);
                if (first != second) {
                    differences++;
                }
            }
        }
        assertTrue(differences > 500, "different seeds must produce different forest stands");
    }
}
