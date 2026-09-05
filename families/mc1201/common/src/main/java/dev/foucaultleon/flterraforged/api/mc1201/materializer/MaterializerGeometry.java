package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;

/** Utility methods for resolving provider-supplied physical terrain and wet geometry. */
public final class MaterializerGeometry {

    /** Numerical tolerance used when comparing physical surface and water planes. */
    public static final double GEOMETRY_EPSILON = 1.0E-6D;

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

    /**
     * Returns whether the provider can physically materialize water for the resolved envelope.
     *
     * <p>A complete fluid cell above the surface is always representable. Water that reaches only
     * into the same Minecraft cell as a partial-height solid requires the provider to advertise
     * waterlogging support; otherwise the host preserves the provider's solid geometry rather than
     * replacing it with a full fluid block.</p>
     *
     * @param materializer active block materializer
     * @param geometry resolved physical surface geometry
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return {@code true} when at least one physical wet cell can be represented
     */
    public static boolean hasMaterializableWater(
            BlockMaterializer materializer,
            MaterializedSurfaceGeometry geometry,
            int waterTopExclusive) {
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(geometry, "geometry");
        if (waterTopExclusive > geometry.blockY() + 1) {
            return true;
        }
        return materializer.capabilities().waterlogging()
                && waterTopExclusive > geometry.topY() + GEOMETRY_EPSILON;
    }

    /**
     * Returns the first Y cell in which the final water envelope may be materialized.
     *
     * <p>For a waterloggable partial top block this may be the same cell as the solid geometry.
     * Otherwise it is the first complete Minecraft cell above the physical top. The returned value
     * may equal or exceed {@code waterTopExclusive}, in which case no materializable water cell is
     * present.</p>
     *
     * @param materializer active block materializer
     * @param geometry resolved physical surface geometry
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return first candidate wet block Y
     */
    public static int firstWaterY(
            BlockMaterializer materializer,
            MaterializedSurfaceGeometry geometry,
            int waterTopExclusive) {
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(geometry, "geometry");
        if (materializer.capabilities().waterlogging()
                && waterTopExclusive > geometry.topY() + GEOMETRY_EPSILON) {
            return geometry.blockY();
        }
        return Math.max(
                geometry.blockY() + 1,
                (int) Math.ceil(geometry.topY() - GEOMETRY_EPSILON));
    }
}
