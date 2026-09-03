package dev.foucaultleon.flterraforged.engine.api.terrain;

import java.util.Objects;

/**
 * Lightweight terrain and hydrology description for placement-time environment checks.
 *
 * <p>The record intentionally omits climate, local gradient and the full river sample. Engine
 * implementations can therefore answer placement probes without executing the complete final
 * terrain sampling path. {@code waterSurfaceHeight} is the continuous hydrologic surface before a
 * Minecraft-family materializer quantizes it to blocks. {@link Double#NaN} denotes a dry column.</p>
 *
 * @param surfaceHeight continuous solid terrain surface height
 * @param waterSurfaceHeight continuous water surface height, or {@link Double#NaN} when dry
 * @param terrainType semantic terrain type at the sampled position
 */
public record TerrainEnvironmentSample(
        double surfaceHeight,
        double waterSurfaceHeight,
        TerrainType terrainType) {

    /**
     * Validates one lightweight environment sample.
     *
     * @param surfaceHeight continuous solid terrain surface height
     * @param waterSurfaceHeight continuous water surface height, or {@link Double#NaN} when dry
     * @param terrainType semantic terrain type at the sampled position
     */
    public TerrainEnvironmentSample {
        if (!Double.isFinite(surfaceHeight)) {
            throw new IllegalArgumentException("surfaceHeight must be finite");
        }
        if (!Double.isNaN(waterSurfaceHeight) && !Double.isFinite(waterSurfaceHeight)) {
            throw new IllegalArgumentException("waterSurfaceHeight must be finite or NaN");
        }
        Objects.requireNonNull(terrainType, "terrainType");
    }

    /**
     * Returns whether the engine reports a hydrologic water surface for this column.
     *
     * @return {@code true} when {@code waterSurfaceHeight} is finite
     */
    public boolean hasWaterSurfaceHeight() {
        return Double.isFinite(waterSurfaceHeight);
    }

    /**
     * Returns continuous water depth above the engine surface before block materialization.
     *
     * @return non-negative water depth, or zero for a dry column
     */
    public double waterDepth() {
        return hasWaterSurfaceHeight()
                ? Math.max(0.0D, waterSurfaceHeight - surfaceHeight)
                : 0.0D;
    }
}
