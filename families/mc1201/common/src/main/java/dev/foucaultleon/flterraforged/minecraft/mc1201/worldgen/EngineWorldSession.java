package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.core.engine.EngineRegistry;
import dev.foucaultleon.flterraforged.engine.api.EngineApiVersion;
import dev.foucaultleon.flterraforged.engine.api.EngineConfig;
import dev.foucaultleon.flterraforged.engine.api.EngineContext;
import dev.foucaultleon.flterraforged.engine.api.EngineId;
import dev.foucaultleon.flterraforged.engine.api.EngineProvider;
import dev.foucaultleon.flterraforged.engine.api.TerrainEngine;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.minecraft.mc1201.access.NoiseConfigSeedAccess;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Lazily binds one configured external engine to the Minecraft world seed.
 *
 * <p>The Minecraft integration owns this lifecycle object. The external engine
 * remains completely unaware of Minecraft classes.</p>
 */
public final class EngineWorldSession implements AutoCloseable {

    private static final EngineRegistry REGISTRY = EngineRegistry.discover(
            EngineWorldSession.class.getClassLoader());

    private final EngineProvider provider;
    private final EngineConfig config;
    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;

    private TerrainEngine engine;
    private volatile TerrainWorld world;
    private volatile EngineContext context;

    /**
     * Creates a lazily initialized engine session.
     *
     * @param engineId engine provider identifier
     * @param config engine configuration values
     * @param minY inclusive world minimum height
     * @param maxYExclusive exclusive world maximum height
     * @param seaLevel Minecraft sea level
     */
    public EngineWorldSession(
            String engineId,
            Map<String, String> config,
            int minY,
            int maxYExclusive,
            int seaLevel) {
        this.provider = REGISTRY.require(EngineId.parse(Objects.requireNonNull(engineId, "engineId")));
        this.config = EngineConfig.of(Objects.requireNonNull(config, "config"));
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
        this.seaLevel = seaLevel;

        EngineApiVersion providerApi = provider.apiVersion();
        if (!EngineApiVersion.CURRENT.hasSameMajor(providerApi)) {
            throw new IllegalStateException(
                    "Engine '" + provider.id() + "' uses incompatible API " + providerApi
                            + "; FlTerraForged expects " + EngineApiVersion.CURRENT);
        }
    }

    /**
     * Returns the terrain world bound to the seed carried by {@code noiseConfig}.
     *
     * @param noiseConfig Minecraft noise configuration for the active world
     * @return world-scoped external terrain sampler
     */
    public TerrainWorld bind(NoiseConfig noiseConfig) {
        Objects.requireNonNull(noiseConfig, "noiseConfig");
        NoiseConfigSeedAccess seedAccess = seedAccess(noiseConfig);

        EngineContext requested = new EngineContext(
                seedAccess.flterraforged$getSeed(), minY, maxYExclusive, seaLevel);
        TerrainWorld currentWorld = world;
        if (currentWorld != null && requested.equals(context)) {
            return currentWorld;
        }

        synchronized (this) {
            currentWorld = world;
            if (currentWorld != null && requested.equals(context)) {
                return currentWorld;
            }
            if (currentWorld != null) {
                throw new IllegalStateException(
                        "Engine session is already bound to " + context
                                + " and cannot switch concurrently to " + requested);
            }
            TerrainEngine createdEngine = provider.create(config);
            if (!EngineApiVersion.CURRENT.hasSameMajor(createdEngine.apiVersion())) {
                createdEngine.close();
                throw new IllegalStateException(
                        "Engine instance from '" + provider.id() + "' uses incompatible API "
                                + createdEngine.apiVersion());
            }
            TerrainWorld createdWorld;
            try {
                createdWorld = createdEngine.openWorld(requested);
            } catch (RuntimeException | Error failure) {
                createdEngine.close();
                throw failure;
            }
            engine = createdEngine;
            context = requested;
            world = createdWorld;
            return createdWorld;
        }
    }

    /**
     * Returns the already seed-bound terrain view during the later feature stage.
     *
     * @return current world-scoped terrain sampler
     * @throws IllegalStateException when no earlier chunk stage has bound the session
     */
    public TerrainWorld boundWorld() {
        TerrainWorld currentWorld = world;
        if (currentWorld == null) {
            throw new IllegalStateException("Engine world is not bound before feature decoration");
        }
        return currentWorld;
    }


    private static NoiseConfigSeedAccess seedAccess(NoiseConfig noiseConfig) {
        try {
            // Mixin adds NoiseConfigSeedAccess to the otherwise final Minecraft class at runtime.
            // Cast through Object so javac does not reject the bridge as statically impossible.
            return (NoiseConfigSeedAccess) (Object) noiseConfig;
        } catch (ClassCastException exception) {
            throw new IllegalStateException(
                    "NoiseConfig seed accessor is unavailable; verify flterraforged.mixins.json",
                    exception);
        }
    }

    /**
     * Returns the selected engine provider identifier.
     *
     * @return selected provider identifier
     */
    public EngineId providerId() {
        return provider.id();
    }

    /**
     * Returns the external engine implementation version reported by the provider.
     *
     * @return provider-reported engine implementation version
     */
    public String providerVersion() {
        return provider.engineVersion();
    }

    @Override
    public synchronized void close() {
        closeBoundResources();
    }

    private void closeBoundResources() {
        TerrainWorld currentWorld = world;
        world = null;
        context = null;
        if (currentWorld != null) {
            currentWorld.close();
        }
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }
}
