package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;

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
     * Tests a position for conservative marine depth without requiring callers to materialize a
     * complete terrain sample.
     *
     * <p>Implementations may override this method with a lightweight pre-hydrology query. The
     * default remains source- and binary-compatible with existing providers and derives the answer
     * from {@link #sample(int, int)}.</p>
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @param minimumDepth required water depth below the world's sea level
     * @return {@code true} when the position is marine terrain with at least the requested depth
     */
    default boolean isMarine(int x, int z, double minimumDepth) {
        if (!Double.isFinite(minimumDepth) || minimumDepth < 0.0D) {
            throw new IllegalArgumentException("minimumDepth must be finite and >= 0");
        }
        TerrainSample sample = sample(x, z);
        boolean marine = StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType());
        return marine && context().seaLevel() - sample.surfaceHeight() >= minimumDepth;
    }

    /** Releases world-scoped resources. */
    @Override
    default void close() {
        // Worlds without resources need no shutdown work.
    }
}
