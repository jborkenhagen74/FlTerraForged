package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerHeightQuantizer;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.Objects;

/**
 * Bounded multi-stage cache for marine structure environment checks.
 *
 * <p>Level one stores resolved materialized columns. Level two stores only the ring that a rule
 * actually requests. The center is always evaluated first, so rejected inland/shallow starts never
 * pay for surrounding hydrology. Ordinary shipwreck, ocean-ruin and ocean-portal starts request four
 * cardinal neighbors instead of the former twelve-neighbor summary; only monuments request the
 * second outer ring.</p>
 *
 * <p>Cold keys use synchronous single-flight ownership. One world-generation thread computes each
 * missing column or ring directly and concurrent callers for the same key reuse its completed result.
 * Loaders do not submit executor work and do not execute while a completed-cache monitor is held. The
 * dependency graph is strictly one-way: {@code ring -> column -> TerrainWorld.environment}.</p>
 *
 * <p>R52 uses the same provider-aware wet geometry as normal chunk materialization. In particular,
 * an overlapping water plane does not turn a non-waterloggable partial-height top cell into marine
 * water merely because both occupy the same integer Minecraft cell.</p>
 */
final class MarineEnvironmentCache {

    private static final int COLUMN_CACHE_SIZE = 8192;
    private static final int RING_CACHE_SIZE = 4096;
    private static final int INNER_RADIUS = 32;
    private static final int OUTER_RADIUS = 64;
    private static final int INNER_RING = 0;
    private static final int OUTER_RING = 1;

    private static final int[][] INNER_OFFSETS = {
        {0, -INNER_RADIUS},
        {-INNER_RADIUS, 0},
        {INNER_RADIUS, 0},
        {0, INNER_RADIUS}
    };
    private static final int[][] OUTER_OFFSETS = {
        {0, -OUTER_RADIUS},
        {-OUTER_RADIUS, 0},
        {OUTER_RADIUS, 0},
        {0, OUTER_RADIUS}
    };

    private final BlockMaterializer materializer;
    private final WorldgenSingleFlightCache<ColumnKey, MarineColumn> columns =
            new WorldgenSingleFlightCache<>(COLUMN_CACHE_SIZE);
    private final WorldgenSingleFlightCache<RingKey, RingStats> rings =
            new WorldgenSingleFlightCache<>(RING_CACHE_SIZE);

    MarineEnvironmentCache(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    MarineColumn column(TerrainWorld world, int x, int z) {
        Objects.requireNonNull(world, "world");
        ColumnKey key = new ColumnKey(world, x, z);
        return columns.get(key, () -> resolveColumn(world, x, z));
    }

    RingStats innerRing(TerrainWorld world, int centerX, int centerZ) {
        return ring(world, centerX, centerZ, INNER_RING, INNER_OFFSETS);
    }

    RingStats outerRing(TerrainWorld world, int centerX, int centerZ) {
        return ring(world, centerX, centerZ, OUTER_RING, OUTER_OFFSETS);
    }

    int cachedColumns() {
        return columns.completedSize();
    }

    int cachedSummaries() {
        return rings.completedSize();
    }

    private RingStats ring(
            TerrainWorld world,
            int centerX,
            int centerZ,
            int ringId,
            int[][] offsets) {
        Objects.requireNonNull(world, "world");
        RingKey key = new RingKey(world, centerX, centerZ, ringId);
        return rings.get(key, () -> sampleRing(world, centerX, centerZ, offsets));
    }

    private MarineColumn resolveColumn(TerrainWorld world, int x, int z) {
        TerrainEnvironmentSample sample = world.environment(x, z);
        TerrainType terrainType = sample.terrainType();
        boolean ocean = StandardTerrainTypes.OCEAN.equals(terrainType);
        boolean coast = StandardTerrainTypes.COAST.equals(terrainType);
        boolean inlandWater = StandardTerrainTypes.RIVER.equals(terrainType)
                || StandardTerrainTypes.LAKE.equals(terrainType)
                || StandardTerrainTypes.LAKE_SHORE.equals(terrainType);
        MaterializedSurfaceGeometry geometry =
                MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
        int waterTop = materializedWaterTop(sample);
        boolean materializedWater = waterTop != Integer.MIN_VALUE
                && MaterializerGeometry.hasMaterializableWater(
                        materializer,
                        geometry,
                        waterTop);
        double waterDepth = materializedWater
                ? Math.max(0.0D, waterTop - geometry.topY())
                : 0.0D;
        return new MarineColumn(
                sample,
                geometry,
                ocean,
                coast,
                inlandWater,
                materializedWater,
                waterDepth);
    }

    private int materializedWaterTop(TerrainEnvironmentSample sample) {
        if (!sample.hasWaterSurfaceHeight()) {
            return Integer.MIN_VALUE;
        }
        int waterTop = MaterializerHeightQuantizer.exclusiveFluidTop(
                sample.waterSurfaceHeight());
        waterTop = Math.max(materializer.context().minY(), waterTop);
        return Math.min(materializer.context().maxYExclusive(), waterTop);
    }

    private RingStats sampleRing(
            TerrainWorld world,
            int centerX,
            int centerZ,
            int[][] offsets) {
        int marineWater = 0;
        int oceanWater = 0;
        int coast = 0;
        int inlandWater = 0;
        double minimumMarineDepth = Double.POSITIVE_INFINITY;
        for (int[] offset : offsets) {
            MarineColumn column = column(world, centerX + offset[0], centerZ + offset[1]);
            if (column.inlandWater()) {
                inlandWater++;
            }
            if (column.coast()) {
                coast++;
            }
            if (column.isMarineWater()) {
                marineWater++;
                minimumMarineDepth = Math.min(minimumMarineDepth, column.waterDepth());
                if (column.ocean()) {
                    oceanWater++;
                }
            }
        }
        if (marineWater == 0) {
            minimumMarineDepth = 0.0D;
        }
        return new RingStats(
                offsets.length,
                marineWater,
                oceanWater,
                coast,
                inlandWater,
                minimumMarineDepth);
    }

    /** Materialized semantic information for one sampled X/Z column. */
    record MarineColumn(
            TerrainEnvironmentSample sample,
            MaterializedSurfaceGeometry geometry,
            boolean ocean,
            boolean coast,
            boolean inlandWater,
            boolean materializedWater,
            double waterDepth) {

        boolean isMarineWater() {
            return materializedWater && (ocean || coast);
        }
    }

    /** Aggregated ring statistics without retaining individual sampled columns. */
    record RingStats(
            int samples,
            int marineWater,
            int oceanWater,
            int coast,
            int inlandWater,
            double minimumMarineDepth) {

        double marineFraction() {
            return samples == 0 ? 0.0D : (double) marineWater / samples;
        }

        double oceanFraction() {
            return samples == 0 ? 0.0D : (double) oceanWater / samples;
        }
    }

    private record ColumnKey(TerrainWorld world, int x, int z) {
        ColumnKey {
            Objects.requireNonNull(world, "world");
        }
    }

    private record RingKey(TerrainWorld world, int x, int z, int ringId) {
        RingKey {
            Objects.requireNonNull(world, "world");
            if (ringId != INNER_RING && ringId != OUTER_RING) {
                throw new IllegalArgumentException("unknown marine ring: " + ringId);
            }
        }
    }
}
