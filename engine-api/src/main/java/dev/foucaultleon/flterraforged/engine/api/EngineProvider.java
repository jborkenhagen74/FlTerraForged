package dev.foucaultleon.flterraforged.engine.api;

/**
 * Entry point implemented by an external terrain engine.
 *
 * <p>Providers are discovered by the FlTerraForged integration layer, normally
 * through Java {@link java.util.ServiceLoader}.</p>
 */
public interface EngineProvider {

    EngineId id();

    String displayName();

    String engineVersion();

    EngineApiVersion apiVersion();

    TerrainEngine create(EngineConfig config);
}
