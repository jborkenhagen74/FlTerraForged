package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;

/**
 * Seeded terrain view for one world.
 *
 * <p>Implementations must make {@link #sample(int, int)} deterministic,
 * order-independent and safe for concurrent calls.</p>
 */
public interface TerrainWorld extends AutoCloseable {

    /**
     * Returns the immutable world context used to create this view.
     *
     * @return world context
     */
    EngineContext context();

    /**
     * Samples terrain data at an X/Z world position.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return terrain sample for the requested position
     */
    TerrainSample sample(int x, int z);

    /**
     * Samples one rectangular square of terrain data in row-major order.
     *
     * <p>The default implementation preserves compatibility with existing providers by delegating
     * to {@link #sample(int, int)}. Providers with tile-aware caches should override this method so
     * a caller that already needs a complete chunk-sized area can avoid repeated cache lookups.</p>
     *
     * @param originX minimum world X coordinate
     * @param originZ minimum world Z coordinate
     * @param size width and depth of the square in blocks
     * @return row-major terrain samples with {@code size * size} entries
     */
    default TerrainSample[] sampleTile(int originX, int originZ, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        TerrainSample[] samples = new TerrainSample[size * size];
        for (int localZ = 0; localZ < size; localZ++) {
            for (int localX = 0; localX < size; localX++) {
                samples[localZ * size + localX] = sample(originX + localX, originZ + localZ);
            }
        }
        return samples;
    }

    /**
     * Samples only the terrain and hydrology data needed for placement-time environment checks.
     *
     * <p>Implementations should override this method when they can answer the query without running
     * climate, local-gradient or other final-sample stages. The default preserves source and binary
     * compatibility with existing engine providers by deriving the result from {@link #sample(int,
     * int)}.</p>
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return lightweight environment sample
     */
    default TerrainEnvironmentSample environment(int x, int z) {
        TerrainSample sample = sample(x, z);
        TerrainType terrain = sample.terrainType();
        double waterSurface = Double.NaN;
        if ((StandardTerrainTypes.OCEAN.equals(terrain)
                        || StandardTerrainTypes.COAST.equals(terrain))
                && sample.surfaceHeight() < context().seaLevel()) {
            waterSurface = context().seaLevel();
        }
        RiverSample hydrology = sample.river();
        if (hydrology.hasWaterSurfaceHeight()
                && hydrology.waterSurfaceHeight() > sample.surfaceHeight()) {
            waterSurface = Double.isFinite(waterSurface)
                    ? Math.max(waterSurface, hydrology.waterSurfaceHeight())
                    : hydrology.waterSurfaceHeight();
        }
        return new TerrainEnvironmentSample(sample.surfaceHeight(), waterSurface, terrain);
    }

    /** Releases world-scoped resources. */
    @Override
    default void close() {
        // Worlds without resources need no shutdown work.
    }
}
