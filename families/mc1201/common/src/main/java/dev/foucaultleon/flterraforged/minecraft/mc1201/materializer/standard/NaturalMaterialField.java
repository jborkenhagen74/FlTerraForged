package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

/** Deterministic low-frequency domain-warped field used for coherent sediment formations. */
final class NaturalMaterialField {

    private static final long WARP_X_SALT = 0x68E31DA4B5297A4DL;
    private static final long WARP_Z_SALT = 0xB4F0A7C15D293E68L;
    private static final long DETAIL_SALT = 0x93C467E37DB0C7A5L;

    private NaturalMaterialField() {
    }

    /**
     * Returns one continuous value in {@code [0,1]} for a broad geological formation.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param salt deterministic formation salt
     * @param scale approximate formation scale in blocks
     * @return continuous formation value in {@code [0,1]}
     */
    static double sample(int x, int z, long salt, double scale) {
        double safeScale = Math.max(8.0D, scale);
        double warpX = (valueNoise(x / 128.0D, z / 128.0D, salt ^ WARP_X_SALT) - 0.5D) * 28.0D;
        double warpZ = (valueNoise(x / 128.0D, z / 128.0D, salt ^ WARP_Z_SALT) - 0.5D) * 28.0D;
        double broad = valueNoise(
                (x + warpX) / safeScale,
                (z + warpZ) / safeScale,
                salt);
        double detail = valueNoise(
                (x - warpZ) / (safeScale * 0.43D),
                (z + warpX) / (safeScale * 0.43D),
                salt ^ DETAIL_SALT);
        return clamp(broad * 0.78D + detail * 0.22D);
    }

    /**
     * Returns a sparse point selector modulated by a continuous habitat field.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param salt deterministic selector salt
     * @param spacing minimum selector-cell size
     * @param habitat normalized local placement likelihood
     * @return {@code true} for the selected habitat anchor
     */
    static boolean sparse(int x, int z, long salt, int spacing, double habitat) {
        int safeSpacing = Math.max(2, spacing);
        int cellX = Math.floorDiv(x, safeSpacing);
        int cellZ = Math.floorDiv(z, safeSpacing);
        long value = mix(salt
                ^ (long) cellX * 0x9E3779B97F4A7C15L
                ^ (long) cellZ * 0xC2B2AE3D27D4EB4FL);
        int anchorX = cellX * safeSpacing + Math.floorMod((int) value, safeSpacing);
        int anchorZ = cellZ * safeSpacing
                + Math.floorMod((int) (value >>> 32), safeSpacing);
        return x == anchorX
                && z == anchorZ
                && sample(x, z, salt ^ DETAIL_SALT, safeSpacing * 3.5D) < clamp(habitat);
    }

    private static double valueNoise(double x, double z, long salt) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double tx = smooth(x - xi);
        double tz = smooth(z - zi);
        double a = hash(xi, zi, salt);
        double b = hash(xi + 1, zi, salt);
        double c = hash(xi, zi + 1, salt);
        double d = hash(xi + 1, zi + 1, salt);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private static double hash(int x, int z, long salt) {
        long value = mix(salt
                ^ (long) x * 0x9E3779B97F4A7C15L
                ^ (long) z * 0xC2B2AE3D27D4EB4FL);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double first, double second, double alpha) {
        return first + (second - first) * alpha;
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
