package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
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

    /**
     * Resolves placement-time physical geometry from a lightweight Engine environment sample.
     *
     * <p>Variable-height providers should override this overload when their surface occupancy can be
     * resolved without a full {@link TerrainSample}. The default deliberately assumes one full block
     * and therefore preserves existing providers while allowing structure checks to remain detached
     * from the expensive final terrain pipeline. R49 uses the same near-integer quantization as the
     * standard materializer so placement checks cannot disagree with generation by one whole block
     * because of floating-point noise.</p>
     *
     * @param sample lightweight Engine environment sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return placement-time realized top-surface geometry
     */
    default MaterializedSurfaceGeometry surfaceGeometry(
            TerrainEnvironmentSample sample,
            int x,
            int z) {
        return MaterializedSurfaceGeometry.fullBlock(
                MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight()));
    }
}
