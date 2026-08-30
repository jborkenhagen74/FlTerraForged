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
    private TerrainWorld world;
    private EngineContext context;

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
        if (!(noiseConfig instanceof NoiseConfigSeedAccess seedAccess)) {
            throw new IllegalStateException(
                    "NoiseConfig seed accessor is unavailable; verify flterraforged.mixins.json");
        }

        EngineContext requested = new EngineContext(
                seedAccess.flterraforged$getSeed(), minY, maxYExclusive, seaLevel);

        synchronized (this) {
            if (requested.equals(context) && world != null) {
                return world;
            }
            closeBoundResources();
            engine = provider.create(config);
            if (!EngineApiVersion.CURRENT.hasSameMajor(engine.apiVersion())) {
                closeBoundResources();
                throw new IllegalStateException(
                        "Engine instance from '" + provider.id() + "' uses incompatible API "
                                + provider.apiVersion());
            }
            world = engine.openWorld(requested);
            context = requested;
            return world;
        }
    }

    /** Returns the selected engine provider identifier. */
    public EngineId providerId() {
        return provider.id();
    }

    @Override
    public synchronized void close() {
        closeBoundResources();
    }

    private void closeBoundResources() {
        if (world != null) {
            world.close();
            world = null;
        }
        if (engine != null) {
            engine.close();
            engine = null;
        }
        context = null;
    }
}
