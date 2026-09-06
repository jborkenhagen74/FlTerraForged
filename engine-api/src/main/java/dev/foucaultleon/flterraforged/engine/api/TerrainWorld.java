package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.chunk.ChunkSnapshot;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Seeded natural-world view for one world.
 *
 * <p>Implementations must make sampling deterministic, order-independent and safe for concurrent
 * calls. API 0.2 makes the Engine the sole owner of natural chunk geometry. Later revisions add
 * source-compatible coarse-placement and aligned bulk-sampling hooks so hosts can avoid duplicate
 * cold work without learning about Engine-internal cache stages.</p>
 */
public interface TerrainWorld extends AutoCloseable {

    /** Returns the immutable world context used to create this view. */
    EngineContext context();

    /**
     * Samples exact final terrain data at an X/Z world position.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return final terrain sample for the requested position
     */
    TerrainSample sample(int x, int z);

    /**
     * Samples terrain for broad host placement decisions without requiring final local hydrology.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return deterministic placement-stage terrain sample
     */
    default TerrainSample placementSample(int x, int z) {
        return sample(x, z);
    }

    /**
     * Returns all exact final X/Z terrain samples for one aligned 16x16 chunk.
     *
     * <p>The returned array is a caller-owned snapshot in local Z-major order. Engines with a
     * chunk-aligned final-sample cache should override this method so a host biome pass and later
     * complete chunk generation share one canonical 2D computation instead of performing 256
     * independent cache lookups. The default implementation preserves compatibility.</p>
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return exactly 256 final terrain samples in local Z-major order
     */
    default TerrainSample[] sampleChunk(int chunkX, int chunkZ) {
        TerrainSample[] samples = new TerrainSample[256];
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                samples[localZ * 16 + localX] = sample(originX + localX, originZ + localZ);
            }
        }
        return samples;
    }

    /**
     * Returns the immutable, complete natural-world snapshot for one chunk.
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
