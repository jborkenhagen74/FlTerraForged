package dev.foucaultleon.flterraforged.engine.api.terrain;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import java.util.Objects;

/**
 * Engine result for one X/Z position.
 *
 * <p>{@code surfaceHeight} is mandatory and continuous. Optional numeric values
 * use {@link Double#NaN} when their capability is unavailable.</p>
 *
 * @param surfaceHeight continuous surface height in block coordinates
 * @param slope engine-defined terrain slope value, or {@link Double#NaN}
 * @param erosion engine-defined erosion value, or {@link Double#NaN}
 * @param continentalness engine-defined continentalness value, or {@link Double#NaN}
 * @param terrainType semantic terrain type
 * @param climate climate sample or {@link ClimateSample#UNAVAILABLE}
 * @param river river sample or {@link RiverSample#UNAVAILABLE}
 */
public record TerrainSample(
        double surfaceHeight,
        double slope,
        double erosion,
        double continentalness,
        TerrainType terrainType,
        ClimateSample climate,
        RiverSample river) {

    /**
     * Creates and validates a terrain sample.
     *
     * @param surfaceHeight continuous surface height in block coordinates
     * @param slope engine-defined terrain slope value, or {@link Double#NaN}
     * @param erosion engine-defined erosion value, or {@link Double#NaN}
     * @param continentalness engine-defined continentalness value, or {@link Double#NaN}
     * @param terrainType semantic terrain type
     * @param climate climate sample or {@link ClimateSample#UNAVAILABLE}
     * @param river river sample or {@link RiverSample#UNAVAILABLE}
     * @throws IllegalArgumentException if {@code surfaceHeight} is not finite
     * @throws NullPointerException if a semantic sample object is {@code null}
     */
    public TerrainSample {
        if (!Double.isFinite(surfaceHeight)) {
            throw new IllegalArgumentException("surfaceHeight must be finite");
        }
        terrainType = Objects.requireNonNull(terrainType, "terrainType");
        climate = Objects.requireNonNull(climate, "climate");
        river = Objects.requireNonNull(river, "river");
    }

    /**
     * Creates the smallest valid sample containing only a continuous surface height.
     *
     * @param surfaceHeight continuous surface height
     * @return terrain sample with all optional values marked unavailable
     */
    public static TerrainSample minimal(double surfaceHeight) {
        return new TerrainSample(
                surfaceHeight,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                StandardTerrainTypes.UNKNOWN,
                ClimateSample.UNAVAILABLE,
                RiverSample.UNAVAILABLE);
    }

    /**
     * Tests whether the surface height contains a fractional component.
     *
     * @return {@code true} when the surface height is not an integer value
     */
    public boolean hasFractionalHeight() {
        return surfaceHeight != Math.rint(surfaceHeight);
    }

    /**
     * Tests whether slope data is available.
     *
     * @return {@code true} when slope is finite
     */
    public boolean hasSlope() {
        return Double.isFinite(slope);
    }

    /**
     * Tests whether erosion data is available.
     *
     * @return {@code true} when erosion is finite
     */
    public boolean hasErosion() {
        return Double.isFinite(erosion);
    }

    /**
     * Tests whether continentalness data is available.
     *
     * @return {@code true} when continentalness is finite
     */
    public boolean hasContinentalness() {
        return Double.isFinite(continentalness);
    }
}
