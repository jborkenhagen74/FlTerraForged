package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;

/** Utility methods for resolving optional provider-supplied physical terrain geometry. */
public final class MaterializerGeometry {

    private MaterializerGeometry() {
    }

    /**
     * Resolves the effective physical surface geometry for one column.
     *
     * <p>Variable-height providers can implement {@link SurfaceGeometryMaterializer}. Legacy and
     * full-block providers retain their established behavior through the full-block fallback.</p>
     *
     * @param materializer active block materializer
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return effective physical surface geometry
     */
    public static MaterializedSurfaceGeometry surfaceGeometry(
            BlockMaterializer materializer,
            TerrainSample sample,
            int x,
            int z) {
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(sample, "sample");
        if (materializer instanceof SurfaceGeometryMaterializer geometryMaterializer) {
            return Objects.requireNonNull(
                    geometryMaterializer.surfaceGeometry(sample, x, z),
                    "SurfaceGeometryMaterializer returned null");
        }
        return MaterializedSurfaceGeometry.fullBlock(materializer.solidSurfaceY(sample));
    }
}
