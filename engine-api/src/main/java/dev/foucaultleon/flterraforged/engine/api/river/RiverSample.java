package dev.foucaultleon.flterraforged.engine.api.river;

/**
 * Hydrology information for a terrain point.
 *
 * @param distance signed or unsigned engine-defined distance to the closest river centerline
 * @param width approximate river width in blocks
 * @param depth approximate river incision depth in blocks
 */
public record RiverSample(double distance, double width, double depth) {

    /** Shared marker sample used when river data is unavailable. */
    public static final RiverSample UNAVAILABLE = new RiverSample(
            Double.NaN, Double.NaN, Double.NaN);

    /**
     * Tests whether all hydrology values are available.
     *
     * @return {@code true} when distance, width and depth are finite
     */
    public boolean isAvailable() {
        return Double.isFinite(distance) && Double.isFinite(width) && Double.isFinite(depth);
    }
}
