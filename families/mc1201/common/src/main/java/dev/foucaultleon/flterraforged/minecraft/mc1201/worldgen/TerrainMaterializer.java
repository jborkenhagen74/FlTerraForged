package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Converts continuous Engine terrain and water heights to the vertical resolution supported by a
 * Minecraft host integration.
 *
 * <p>The default 1.20.1 implementation uses full blocks. A Conquest Reforged integration can later
 * provide a finer implementation without changing Engine lake geometry.</p>
 */
public interface TerrainMaterializer {

    /**
     * Returns the smallest vertical terrain step represented by this materializer, in blocks.
     *
     * @return vertical resolution in block units
     */
    double verticalResolution();

    /**
     * Returns whether the materializer can represent partial-height terrain blocks.
     *
     * @return {@code true} when partial blocks are supported
     */
    boolean supportsPartialBlocks();

    /**
     * Returns whether generated partial terrain may carry water through waterlogging.
     *
     * @return {@code true} when waterlogged terrain shapes are supported
     */
    boolean supportsWaterlogging();

    /**
     * Resolves the topmost solid full-block Y used by Minecraft generation for one sample.
     *
     * @param sample continuous Engine sample
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @return materialized topmost solid block Y
     */
    int solidSurfaceY(TerrainSample sample, int minY, int maxYExclusive);

    /**
     * Returns the first Y above the materialized solid surface.
     *
     * @param sample continuous Engine sample
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @return first block above the solid surface
     */
    default int solidSurfaceTop(TerrainSample sample, int minY, int maxYExclusive) {
        return solidSurfaceY(sample, minY, maxYExclusive) + 1;
    }

    /**
     * Returns the first Y above all water belonging to this terrain column.
     *
     * @param sample continuous Engine sample
     * @param seaLevel world sea level
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @return exclusive water top
     */
    int waterTopExclusive(TerrainSample sample, int seaLevel, int minY, int maxYExclusive);

    /**
     * Returns whether the Engine hydrology can be represented by at least one water cell.
     *
     * @param sample continuous Engine sample
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @return {@code true} when material water exists above the solid surface
     */
    boolean hasMaterializedWater(TerrainSample sample, int minY, int maxYExclusive);
}
