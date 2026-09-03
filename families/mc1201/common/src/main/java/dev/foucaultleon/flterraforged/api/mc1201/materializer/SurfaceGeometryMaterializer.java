package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Optional materializer extension for providers that emit non-full-height terrain blocks.
 *
 * <p>The base {@link BlockMaterializer} contract remains source-compatible for existing providers.
 * A provider advertises exact realized surface geometry by additionally implementing this interface.
 * The host falls back to a full block ending at {@code solidSurfaceY + 1} when this extension is not
 * implemented.</p>
 */
public interface SurfaceGeometryMaterializer {

    /**
     * Resolves the physical top geometry for one materialized terrain column.
     *
     * <p>The method must be deterministic and side-effect free. It must not query or generate
     * neighboring chunks. Providers may use their own configured palettes, the Engine sample and
     * world coordinates to select layered, slab-like or otherwise variable-height states.</p>
     *
     * @param sample continuous Engine terrain sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return realized top-surface geometry
     */
    MaterializedSurfaceGeometry surfaceGeometry(TerrainSample sample, int x, int z);
}
