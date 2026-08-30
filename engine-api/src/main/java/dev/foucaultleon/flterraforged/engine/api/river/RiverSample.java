package dev.foucaultleon.flterraforged.engine.api.river;

/**
 * Hydrology information for a terrain point.
 *
 * @param distance signed/unsigned engine-defined distance to the closest river centerline
 * @param width approximate river width in blocks
 * @param depth approximate river incision depth in blocks
 */
public record RiverSample(double distance, double width, double depth) {

    public static final RiverSample UNAVAILABLE = new RiverSample(
            Double.NaN, Double.NaN, Double.NaN);

    public boolean isAvailable() {
        return Double.isFinite(distance) && Double.isFinite(width) && Double.isFinite(depth);
    }
}
