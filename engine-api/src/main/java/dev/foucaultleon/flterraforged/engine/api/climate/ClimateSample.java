package dev.foucaultleon.flterraforged.engine.api.climate;

/** Continuous climate values. NaN denotes unavailable optional data. */
public record ClimateSample(double temperature, double moisture) {

    public static final ClimateSample UNAVAILABLE = new ClimateSample(Double.NaN, Double.NaN);

    public boolean isAvailable() {
        return Double.isFinite(temperature) && Double.isFinite(moisture);
    }
}
