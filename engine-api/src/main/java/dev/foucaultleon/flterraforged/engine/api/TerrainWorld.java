package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.chunk.ChunkSnapshot;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Seeded natural-world view for one world.
 *
 * <p>Implementations must make sampling deterministic, order-independent and safe for concurrent
 * calls. API 0.2 makes the Engine the sole owner of natural chunk geometry. R59 adds the placement
 * sampler as a source-compatible default method so host structure discovery can avoid cold-starting
 * complete hydrology and erosion regions before chunk progress becomes visible.</p>
 */
public interface TerrainWorld extends AutoCloseable {

    /**
     * Returns the immutable world context used to create this view.
     *
     * @return world context
     */
    EngineContext context();

    /**
     * Samples exact final terrain data at an X/Z world position.
     *
     * <p>This query contains final terrain, erosion, hydrology and climate semantics. Full block
     * geometry must be consumed through {@link #chunkSnapshot(int, int)}.</p>
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return final terrain sample for the requested position
     */
    TerrainSample sample(int x, int z);

    /**
     * Samples terrain for broad host placement decisions without requiring final local hydrology.
     *
     * <p>Implementations may use a lower-cost continent, base-terrain and climate path here. The
     * returned value must remain deterministic and preserve broad land/ocean/climate semantics, but
     * it need not contain final river incision, lake fill or physical erosion. Hosts should use
     * this method only for coarse structure/feature discovery and must perform exact environment
     * validation with {@link #sample(int, int)} before accepting sensitive starts.</p>
     *
     * <p>The default implementation preserves compatibility with Engine implementations that have
     * not yet supplied a specialized placement path by falling back to the exact sampler.</p>
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return deterministic placement-stage terrain sample
     */
    default TerrainSample placementSample(int x, int z) {
        return sample(x, z);
    }

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
