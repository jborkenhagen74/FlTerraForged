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
     * Returns whether final water can be represented for a full Engine sample and column.
     *
     * @param materializer active block materializer
     * @param sample continuous Engine sample
     * @param geometry resolved physical surface geometry
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return {@code true} when at least one physical wet cell can be represented
     */
    public static boolean hasMaterializableWater(
            BlockMaterializer materializer,
            TerrainSample sample,
            MaterializedSurfaceGeometry geometry,
            int x,
            int z,
            int waterTopExclusive) {
        Objects.requireNonNull(sample, "sample");
        return hasMaterializableWater(
                materializer,
                geometry,
                waterTopExclusive,
                supportsSameCellWater(materializer, sample, x, z));
    }

    /**
     * Returns whether final water can be represented for a lightweight placement sample.
     *
     * @param materializer active block materializer
     * @param sample lightweight Engine environment sample
     * @param geometry resolved physical surface geometry
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return {@code true} when at least one physical wet cell can be represented
     */
    public static boolean hasMaterializableWater(
            BlockMaterializer materializer,
            TerrainEnvironmentSample sample,
            MaterializedSurfaceGeometry geometry,
            int x,
            int z,
            int waterTopExclusive) {
        Objects.requireNonNull(sample, "sample");
        return hasMaterializableWater(
                materializer,
                geometry,
                waterTopExclusive,
                supportsSameCellWater(materializer, sample, x, z));
    }

    /**
     * Returns the first Y cell in which final water may be represented for a full sample.
     *
     * @param materializer active block materializer
     * @param sample continuous Engine sample
     * @param geometry resolved physical surface geometry
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return first candidate wet block Y
     */
    public static int firstWaterY(
            BlockMaterializer materializer,
            TerrainSample sample,
            MaterializedSurfaceGeometry geometry,
            int x,
            int z,
            int waterTopExclusive) {
        Objects.requireNonNull(sample, "sample");
        return firstWaterY(
                geometry,
                waterTopExclusive,
                supportsSameCellWater(materializer, sample, x, z));
    }

    /**
     * Returns the first Y cell in which final water may be represented for a placement sample.
     *
     * @param materializer active block materializer
     * @param sample lightweight Engine environment sample
     * @param geometry resolved physical surface geometry
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param waterTopExclusive exclusive top of the materialized fluid envelope
     * @return first candidate wet block Y
     */
    public static int firstWaterY(
            BlockMaterializer materializer,
            TerrainEnvironmentSample sample,
            MaterializedSurfaceGeometry geometry,
            int x,
            int z,
            int waterTopExclusive) {
        Objects.requireNonNull(sample, "sample");
        return firstWaterY(
                geometry,
                waterTopExclusive,
                supportsSameCellWater(materializer, sample, x, z));
    }

    private static boolean hasMaterializableWater(
            BlockMaterializer materializer,
            MaterializedSurfaceGeometry geometry,
            int waterTopExclusive,
            boolean supportsSameCellWater) {
        Objects.requireNonNull(materializer, "materializer");
        Objects.requireNonNull(geometry, "geometry");
        if (waterTopExclusive > geometry.blockY() + 1) {
            return true;
        }
        return materializer.capabilities().waterlogging()
                && supportsSameCellWater
                && waterTopExclusive > geometry.topY() + GEOMETRY_EPSILON;
    }

    private static int firstWaterY(
            MaterializedSurfaceGeometry geometry,
            int waterTopExclusive,
            boolean supportsSameCellWater) {
        Objects.requireNonNull(geometry, "geometry");
        if (supportsSameCellWater
                && waterTopExclusive > geometry.topY() + GEOMETRY_EPSILON) {
            return geometry.blockY();
        }
        return Math.max(
                geometry.blockY() + 1,
                (int) Math.ceil(geometry.topY() - GEOMETRY_EPSILON));
    }

    private static boolean supportsSameCellWater(
            BlockMaterializer materializer,
            TerrainSample sample,
            int x,
            int z) {
        Objects.requireNonNull(materializer, "materializer");
        if (!materializer.capabilities().waterlogging()) {
            return false;
        }
        return !(materializer instanceof SurfaceGeometryMaterializer geometryMaterializer)
                || geometryMaterializer.supportsSurfaceWaterlogging(sample, x, z);
    }

    private static boolean supportsSameCellWater(
            BlockMaterializer materializer,
            TerrainEnvironmentSample sample,
            int x,
            int z) {
        Objects.requireNonNull(materializer, "materializer");
        if (!materializer.capabilities().waterlogging()) {
            return false;
        }
        return !(materializer instanceof SurfaceGeometryMaterializer geometryMaterializer)
                || geometryMaterializer.supportsSurfaceWaterlogging(sample, x, z);
    }
}
