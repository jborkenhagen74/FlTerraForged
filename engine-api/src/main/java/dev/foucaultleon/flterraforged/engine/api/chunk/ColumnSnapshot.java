package dev.foucaultleon.flterraforged.engine.api.chunk;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;

/**
 * Immutable natural-world metadata for one X/Z column of an Engine chunk snapshot.
 *
 * @param terrain resolved terrain, climate and hydrology sample
 * @param geology broad geology class
 * @param solidSurfaceY topmost natural solid block before partial-block materialization
 * @param waterTopExclusive first block above the natural water column, or the solid surface top
 *     when the column is dry
 * @param soilDepth number of near-surface soil/filler blocks beneath the visible top
 * @param groundwaterY highest Y that may contain a saturated Engine-owned cave cell
 */
public record ColumnSnapshot(
        TerrainSample terrain,
        GeologyType geology,
        int solidSurfaceY,
        int waterTopExclusive,
        int soilDepth,
        int groundwaterY) {

    /**
     * Creates and validates immutable column metadata.
     *
     * @param terrain resolved terrain, climate and hydrology sample
     * @param geology broad geology class
     * @param solidSurfaceY topmost natural solid block
     * @param waterTopExclusive first block above natural water
     * @param soilDepth number of soil/filler blocks beneath the visible top
     * @param groundwaterY highest saturated cave level
     * @throws NullPointerException when semantic objects are null
     * @throws IllegalArgumentException when soil depth is negative or water is below the surface
     */
    public ColumnSnapshot {
        terrain = Objects.requireNonNull(terrain, "terrain");
        geology = Objects.requireNonNull(geology, "geology");
        if (soilDepth < 0) {
            throw new IllegalArgumentException("soilDepth must be non-negative");
        }
        if (waterTopExclusive < solidSurfaceY + 1) {
            throw new IllegalArgumentException("waterTopExclusive must not be below solid surface top");
        }
    }

    /**
     * Tests whether the column has Engine-owned surface water.
     *
     * @return {@code true} when water exists above the solid surface
     */
    public boolean hasSurfaceWater() {
        return waterTopExclusive > solidSurfaceY + 1;
    }
}
