package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
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

    /**
     * Resolves placement-time physical surface geometry from a lightweight Engine sample.
     *
     * <p>This overload is used by structure-environment checks before normal biome/noise sampling.
     * It never requests a full {@link TerrainSample}. Providers such as Conquest-style materializers
     * can override the lightweight method on {@link SurfaceGeometryMaterializer} to report exact
     * layered or partial-block occupancy.</p>
     *
     * @param materializer active block materializer
     * @param sample lightweight Engine environment sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return effective physical surface geometry
     */
    public static MaterializedSurfaceGeometry surfaceGeometry(
            BlockMaterializer materializer,
            TerrainEnvironmentSample sample,
            int x,
            int z) {
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(sample, "sample");
        if (materializer instanceof SurfaceGeometryMaterializer geometryMaterializer) {
            return Objects.requireNonNull(
                    geometryMaterializer.surfaceGeometry(sample, x, z),
                    "SurfaceGeometryMaterializer returned null");
        }
        int blockY = Math.max(
                materializer.context().minY() + 1,
                Math.min(
                        materializer.context().maxYExclusive() - 2,
                        MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())));
        return MaterializedSurfaceGeometry.fullBlock(blockY);
    }
}
