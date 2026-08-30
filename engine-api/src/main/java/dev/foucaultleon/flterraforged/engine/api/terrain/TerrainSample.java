package dev.foucaultleon.flterraforged.engine.api.terrain;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import java.util.Objects;

/**
 * Engine result for one X/Z position.
 *
 * <p>{@code surfaceHeight} is mandatory and continuous. Optional numeric values
 * use {@link Double#NaN} when their capability is unavailable.</p>
 */
public record TerrainSample(
        double surfaceHeight,
        double slope,
        double erosion,
        double continentalness,
        TerrainType terrainType,
        ClimateSample climate,
        RiverSample river) {

    public TerrainSample {
        if (!Double.isFinite(surfaceHeight)) {
            throw new IllegalArgumentException("surfaceHeight must be finite");
        }
        terrainType = Objects.requireNonNull(terrainType, "terrainType");
        climate = Objects.requireNonNull(climate, "climate");
        river = Objects.requireNonNull(river, "river");
    }

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

    public boolean hasFractionalHeight() {
        return surfaceHeight != Math.rint(surfaceHeight);
    }

    public boolean hasSlope() {
        return Double.isFinite(slope);
    }

    public boolean hasErosion() {
        return Double.isFinite(erosion);
    }

    public boolean hasContinentalness() {
        return Double.isFinite(continentalness);
    }
}
