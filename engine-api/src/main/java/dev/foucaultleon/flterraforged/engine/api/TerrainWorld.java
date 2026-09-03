package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

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

    /** Releases world-scoped resources. */
    @Override
    default void close() {
        // Worlds without resources need no shutdown work.
    }
}
