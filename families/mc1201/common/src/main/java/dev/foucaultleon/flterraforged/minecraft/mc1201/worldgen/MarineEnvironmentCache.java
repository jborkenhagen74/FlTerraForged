package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Bounded multi-stage cache for marine structure environment checks.
 *
 * <p>Level one stores resolved materialized columns. Level two stores the bounded environment
 * stencil shared by every marine structure rule for one start position. Both levels coalesce
 * concurrent cold misses without scheduling additional executor tasks. Engine samples, provider
 * geometry and environment stencils are therefore generated once per key and reused by parallel
 * structure-start workers.</p>
 */
final class MarineEnvironmentCache {

    private static final int COLUMN_CACHE_SIZE = 8192;
    private static final int SUMMARY_CACHE_SIZE = 2048;
    private static final int INNER_RADIUS = 32;
    private static final int OUTER_RADIUS = 64;
    private static final int[][] INNER_OFFSETS = {
        {-INNER_RADIUS, -INNER_RADIUS},
        {0, -INNER_RADIUS},
        {INNER_RADIUS, -INNER_RADIUS},
        {-INNER_RADIUS, 0},
        {INNER_RADIUS, 0},
        {-INNER_RADIUS, INNER_RADIUS},
        {0, INNER_RADIUS},
        {INNER_RADIUS, INNER_RADIUS}
    };
    private static final int[][] OUTER_OFFSETS = {
        {0, -OUTER_RADIUS},
        {-OUTER_RADIUS, 0},
        {OUTER_RADIUS, 0},
        {0, OUTER_RADIUS}
    };

    private final BlockMaterializer materializer;
    private final SingleFlightCache<ColumnKey, MarineColumn> columns =
            new SingleFlightCache<>(COLUMN_CACHE_SIZE);
    private final SingleFlightCache<SummaryKey, MarineEnvironmentSummary> summaries =
            new SingleFlightCache<>(SUMMARY_CACHE_SIZE);

    MarineEnvironmentCache(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    MarineColumn column(TerrainWorld world, int x, int z) {
        Objects.requireNonNull(world, "world");
        ColumnKey key = new ColumnKey(world, x, z);
        return columns.get(key, () -> resolveColumn(world, x, z));
    }

    MarineEnvironmentSummary summary(TerrainWorld world, int centerX, int centerZ) {
        Objects.requireNonNull(world, "world");
        SummaryKey key = new SummaryKey(world, centerX, centerZ);
        return summaries.get(key, () -> resolveSummary(world, centerX, centerZ));
    }

    int cachedColumns() {
        return columns.completedSize();
    }

    int cachedSummaries() {
        return summaries.completedSize();
    }

    private MarineColumn resolveColumn(TerrainWorld world, int x, int z) {
        TerrainSample sample = world.sample(x, z);
        TerrainType terrainType = sample.terrainType();
        boolean ocean = StandardTerrainTypes.OCEAN.equals(terrainType);
        boolean coast = StandardTerrainTypes.COAST.equals(terrainType);
        boolean inlandWater = StandardTerrainTypes.RIVER.equals(terrainType)
                || StandardTerrainTypes.LAKE.equals(terrainType)
                || StandardTerrainTypes.LAKE_SHORE.equals(terrainType);
        boolean materializedWater = materializer.hasMaterializedWater(sample);
        MaterializedSurfaceGeometry geometry =
                MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
        double waterDepth = materializedWater
                ? Math.max(0.0D, materializer.waterTopExclusive(sample) - geometry.topY())
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

    private MarineEnvironmentSummary resolveSummary(TerrainWorld world, int centerX, int centerZ) {
        MarineColumn center = column(world, centerX, centerZ);
        RingStats inner = sampleRing(world, centerX, centerZ, INNER_OFFSETS);
        RingStats outer = sampleRing(world, centerX, centerZ, OUTER_OFFSETS);
        return new MarineEnvironmentSummary(center, inner, outer);
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
            TerrainSample sample,
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

    /** Reusable center plus inner/outer ring environment summary. */
    record MarineEnvironmentSummary(
            MarineColumn center,
            RingStats inner,
            RingStats outer) {
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

    private record SummaryKey(TerrainWorld world, int x, int z) {
        SummaryKey {
            Objects.requireNonNull(world, "world");
        }
    }

    private static final class SingleFlightCache<K, V> {

        private final BoundedMap<K, V> completed;
        private final ConcurrentMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

        SingleFlightCache(int maximumSize) {
            completed = new BoundedMap<>(maximumSize);
        }

        V get(K key, Supplier<V> loader) {
            V cached;
            synchronized (completed) {
                cached = completed.get(key);
            }
            if (cached != null) {
                return cached;
            }

            CompletableFuture<V> owned = new CompletableFuture<>();
            CompletableFuture<V> existing = inFlight.putIfAbsent(key, owned);
            if (existing != null) {
                return await(existing);
            }

            try {
                V loaded = Objects.requireNonNull(loader.get(), "cache loader returned null");
                V retained;
                synchronized (completed) {
                    V secondLook = completed.get(key);
                    if (secondLook == null) {
                        completed.put(key, loaded);
                        retained = loaded;
                    } else {
                        retained = secondLook;
                    }
                }
                owned.complete(retained);
                return retained;
            } catch (Throwable throwable) {
                owned.completeExceptionally(throwable);
                throw propagate(throwable);
            } finally {
                inFlight.remove(key, owned);
            }
        }

        int completedSize() {
            synchronized (completed) {
                return completed.size();
            }
        }

        private static <V> V await(CompletableFuture<V> future) {
            try {
                return future.join();
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause();
                throw propagate(cause == null ? exception : cause);
            }
        }

        private static RuntimeException propagate(Throwable throwable) {
            if (throwable instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            return new IllegalStateException("Marine environment cache load failed", throwable);
        }
    }

    private static final class BoundedMap<K, V> extends LinkedHashMap<K, V> {

        private static final long serialVersionUID = 1L;
        private final int maximumSize;

        BoundedMap(int maximumSize) {
            super(maximumSize + 1, 0.75F, true);
            if (maximumSize < 1) {
                throw new IllegalArgumentException("maximumSize must be >= 1");
            }
            this.maximumSize = maximumSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maximumSize;
        }
    }
}
