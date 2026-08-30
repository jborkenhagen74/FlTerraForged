package dev.foucaultleon.flterraforged.engine.api.climate;

/**
 * Continuous climate values returned by an engine.
 *
 * <p>{@link Double#NaN} denotes unavailable optional data.</p>
 *
 * @param temperature engine-defined normalized or continuous temperature value
 * @param moisture engine-defined normalized or continuous moisture value
 */
public record ClimateSample(double temperature, double moisture) {

    /** Shared marker sample used when climate data is unavailable. */
    public static final ClimateSample UNAVAILABLE = new ClimateSample(Double.NaN, Double.NaN);

    /**
     * Tests whether both climate values are available.
     *
     * @return {@code true} when temperature and moisture are finite
     */
    public boolean isAvailable() {
        return Double.isFinite(temperature) && Double.isFinite(moisture);
    }
}
