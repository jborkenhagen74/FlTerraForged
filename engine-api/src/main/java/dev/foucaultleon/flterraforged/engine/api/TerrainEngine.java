package dev.foucaultleon.flterraforged.engine.api;

/** Runtime instance of an external engine implementation. */
public interface TerrainEngine extends AutoCloseable {

    EngineApiVersion apiVersion();

    EngineCapabilities capabilities();

    TerrainWorld openWorld(EngineContext context);

    @Override
    default void close() {
        // Engines without global resources need no shutdown work.
    }
}
