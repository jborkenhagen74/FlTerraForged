package dev.foucaultleon.flterraforged.engine.api;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Seeded terrain view for one world.
 *
 * <p>Implementations must make {@link #sample(int, int)} deterministic,
 * order-independent and safe for concurrent calls.</p>
 */
public interface TerrainWorld extends AutoCloseable {

    EngineContext context();

    TerrainSample sample(int x, int z);

    @Override
    default void close() {
        // Worlds without resources need no shutdown work.
    }
}
