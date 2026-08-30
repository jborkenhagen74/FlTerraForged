package dev.foucaultleon.flterraforged.engine.api.river;

/**
 * Hydrology information for a terrain point.
 *
 * <p>The core channel geometry ({@code distance}, {@code width} and {@code depth}) remains
 * available to engines implementing the original 0.1 API. Water-surface height and accumulated
 * flow are additive hydrology signals and use {@link Double#NaN} when an engine does not provide
 * them.</p>
 *
 * @param distance signed or unsigned engine-defined distance to the closest river centerline
 * @param width approximate river width in blocks
 * @param depth approximate local river incision depth in blocks
 * @param waterSurfaceHeight continuous world-space Y coordinate of the local river water surface,
 *        or {@link Double#NaN} when unavailable
 * @param flow accumulated engine-defined drainage flow, or {@link Double#NaN} when unavailable
 */
public record RiverSample(
        double distance,
        double width,
        double depth,
        double waterSurfaceHeight,
        double flow) {

    /** Shared marker sample used when river data is unavailable. */
    public static final RiverSample UNAVAILABLE = new RiverSample(
            Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    /**
     * Creates the legacy three-value hydrology representation.
     *
     * <p>This constructor deliberately remains available so engines compiled against the original
     * 0.1 API keep linking. The newer water-surface and flow values are marked unavailable.</p>
     *
     * @param distance distance to the closest river centerline
     * @param width approximate river width in blocks
     * @param depth approximate local river incision depth in blocks
     */
    public RiverSample(double distance, double width, double depth) {
        this(distance, width, depth, Double.NaN, Double.NaN);
    }

    /**
     * Tests whether the core channel-geometry values are available.
     *
     * @return {@code true} when distance, width and depth are finite
     */
    public boolean isAvailable() {
        return Double.isFinite(distance) && Double.isFinite(width) && Double.isFinite(depth);
    }

    /**
     * Tests whether a hydrologically coherent river-water surface is available.
     *
     * @return {@code true} when the water-surface height is finite
     */
    public boolean hasWaterSurfaceHeight() {
        return Double.isFinite(waterSurfaceHeight);
    }

    /**
     * Tests whether accumulated drainage-flow information is available.
     *
     * @return {@code true} when flow is finite
     */
    public boolean hasFlow() {
        return Double.isFinite(flow);
    }
}
