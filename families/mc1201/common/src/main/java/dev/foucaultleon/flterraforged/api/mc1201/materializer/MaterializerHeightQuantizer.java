package dev.foucaultleon.flterraforged.api.mc1201.materializer;

/**
 * Canonical conversion between continuous Engine heights and Minecraft block coordinates.
 *
 * <p>World-generation math frequently produces values that are mathematically integral but differ
 * from the intended integer by a few floating-point ulps. Applying {@code Math.floor} directly to
 * values such as {@code 62.999999999} would therefore lower an entire Minecraft layer. This helper
 * first snaps only values extremely close to an integer and leaves deliberately fractional heights
 * unchanged.</p>
 */
public final class MaterializerHeightQuantizer {

    /** Maximum distance from an integer that is treated as floating-point noise. */
    public static final double INTEGER_EPSILON = 1.0E-6D;

    private MaterializerHeightQuantizer() {
    }

    /**
     * Snaps a finite value to its nearest integer only inside the numerical tolerance.
     *
     * @param value continuous world-space value
     * @return stable continuous value for subsequent block quantization
     */
    public static double snapNearInteger(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("height value must be finite");
        }
        double nearest = Math.rint(value);
        return Math.abs(value - nearest) <= INTEGER_EPSILON ? nearest : value;
    }

    /**
     * Returns the block cell containing a continuous Engine height.
     *
     * @param value continuous world-space height
     * @return stable floor block coordinate
     */
    public static int floorBlock(double value) {
        return (int) Math.floor(snapNearInteger(value));
    }

    /**
     * Returns the exclusive top for full Minecraft fluid cells at a continuous water level.
     *
     * <p>An integral Engine water level still owns the block cell at that Y coordinate, matching the
     * existing FlTerraForged convention. Fractional levels own every complete cell below them.</p>
     *
     * @param waterSurfaceHeight continuous Engine water-surface height
     * @return exclusive top block coordinate for materialized fluid
     */
    public static int exclusiveFluidTop(double waterSurfaceHeight) {
        return floorBlock(waterSurfaceHeight) + 1;
    }
}
