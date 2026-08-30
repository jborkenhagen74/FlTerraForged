package dev.foucaultleon.flterraforged.engine.api;

/** Runtime instance of an external engine implementation. */
public interface TerrainEngine extends AutoCloseable {

    /**
     * Returns the engine API version implemented by this engine.
     *
     * @return engine API version
     */
    EngineApiVersion apiVersion();

    /**
     * Returns the optional capabilities exposed by this engine.
     *
     * @return immutable capability set
     */
    EngineCapabilities capabilities();

    /**
     * Opens a deterministic terrain view for a world.
     *
     * @param context immutable world context
     * @return world-scoped terrain sampler
     */
    TerrainWorld openWorld(EngineContext context);

    /** Releases engine-wide resources. */
    @Override
    default void close() {
        // Engines without global resources need no shutdown work.
    }
}
