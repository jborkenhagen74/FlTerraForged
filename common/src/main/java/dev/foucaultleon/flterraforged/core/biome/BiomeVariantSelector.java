package dev.foucaultleon.flterraforged.core.biome;

import java.util.Objects;

/** Selects broad, seed-dependent native-biome stands inside one semantic biome role. */
public final class BiomeVariantSelector {

    private static final double CELL_SIZE = 384.0D;
    private static final double JITTER_MINIMUM = 0.14D;
    private static final double JITTER_RANGE = 0.72D;

    private BiomeVariantSelector() {
    }

    /**
     * Selects a deterministic candidate in an irregular nearest-cell mosaic.
     *
     * <p>The cells are intentionally several hundred blocks wide. A native forest biome therefore
     * forms a recognizable stand instead of changing tree species every few blocks, while the
     * uniform cell hash prevents one candidate such as birch forest from dominating thousands of
     * blocks merely because the local climate signals occupy a narrow numeric interval.</p>
     *
     * @param role semantic biome role
     * @param candidateCount number of native candidates or weighted candidate entries
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param seed world seed
     * @return candidate index in {@code [0, candidateCount)}
     */
    public static int index(
            BiomeRole role,
            int candidateCount,
            int x,
            int z,
            long seed) {
        Objects.requireNonNull(role, "role");
        if (candidateCount < 1) {
            throw new IllegalArgumentException("candidateCount must be >= 1");
        }
        if (candidateCount == 1) {
            return 0;
        }

        double gridX = x / CELL_SIZE;
        double gridZ = z / CELL_SIZE;
        int baseX = (int) Math.floor(gridX);
        int baseZ = (int) Math.floor(gridZ);
        double nearestDistance = Double.POSITIVE_INFINITY;
        long winningHash = 0L;
        long roleSalt = (long) (role.ordinal() + 1) * 0xD6E8FEB86659FD93L;
        for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                int cellX = baseX + offsetX;
                int cellZ = baseZ + offsetZ;
                long hash = mix(seed
                        ^ roleSalt
                        ^ (long) cellX * 0x9E3779B97F4A7C15L
                        ^ (long) cellZ * 0xC2B2AE3D27D4EB4FL);
                double centerX = cellX + JITTER_MINIMUM
                        + unit(hash) * JITTER_RANGE;
                double centerZ = cellZ + JITTER_MINIMUM
                        + unit(mix(hash ^ 0xA5A3564E27F3A21DL)) * JITTER_RANGE;
                double dx = gridX - centerX;
                double dz = gridZ - centerZ;
                double distance = dx * dx + dz * dz;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    winningHash = hash;
                }
            }
        }
        return (int) Math.floor(unit(mix(winningHash ^ 0x94D049BB133111EBL)) * candidateCount);
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
