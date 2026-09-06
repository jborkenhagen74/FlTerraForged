package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.chunk.ChunkSnapshot;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Seeded natural-world view for one world.
 *
 * <p>Implementations must make sampling deterministic, order-independent and safe for concurrent
 * calls. API 0.2 makes the Engine the sole owner of natural chunk geometry: terrain, hydrology,
 * geology, caves, underground fluids and the world floor are resolved in {@link #chunkSnapshot}.</p>
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
     * <p>This lightweight query exists for biome routing, height queries and structure suitability.
     * Full block geometry must be consumed through {@link #chunkSnapshot(int, int)}.</p>
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return terrain sample for the requested position
     */
    TerrainSample sample(int x, int z);

    /**
     * Returns the immutable, complete natural-world snapshot for one chunk.
     *
     * <p>Concurrent requests for the same coordinates must share one generation result. Published
     * snapshots must never mutate and must not call back into host world-generation code.</p>
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return complete natural chunk snapshot
     */
    ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ);

    /** Releases world-scoped resources. */
    @Override
    default void close() {
        // Worlds without resources need no shutdown work.
    }
}
