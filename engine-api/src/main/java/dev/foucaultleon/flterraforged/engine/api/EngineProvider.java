package dev.foucaultleon.flterraforged.engine.api;

/**
 * Entry point implemented by an external terrain engine.
 *
 * <p>Providers are discovered by the FlTerraForged integration layer, normally
 * through Java {@link java.util.ServiceLoader}.</p>
 */
public interface EngineProvider {

    /**
     * Returns the globally unique provider identifier.
     *
     * @return provider identifier
     */
    EngineId id();

    /**
     * Returns a human-readable provider name.
     *
     * @return display name
     */
    String displayName();

    /**
     * Returns the engine implementation version.
     *
     * @return implementation version string
     */
    String engineVersion();

    /**
     * Returns the engine API version implemented by this provider.
     *
     * @return supported engine API version
     */
    EngineApiVersion apiVersion();

    /**
     * Creates a new terrain engine instance.
     *
     * @param config engine-specific configuration
     * @return configured terrain engine
     */
    TerrainEngine create(EngineConfig config);
}
