package dev.foucaultleon.flterraforged.core.engine;

import dev.foucaultleon.flterraforged.engine.api.EngineId;
import dev.foucaultleon.flterraforged.engine.api.EngineProvider;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/** Discovers and validates external engine providers. */
public final class EngineRegistry {

    private final Map<EngineId, EngineProvider> providers = new LinkedHashMap<>();

    public static EngineRegistry discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        EngineRegistry registry = new EngineRegistry();
        ServiceLoader.load(EngineProvider.class, classLoader).forEach(registry::register);
        return registry;
    }

    public void register(EngineProvider provider) {
        Objects.requireNonNull(provider, "provider");
        EngineProvider previous = providers.putIfAbsent(provider.id(), provider);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate engine provider id '" + provider.id() + "': "
                            + previous.getClass().getName() + " and "
                            + provider.getClass().getName());
        }
    }

    public EngineProvider require(EngineId id) {
        EngineProvider provider = providers.get(Objects.requireNonNull(id, "id"));
        if (provider == null) {
            throw new IllegalStateException("No engine provider registered for '" + id + "'");
        }
        return provider;
    }

    public Collection<EngineProvider> providers() {
        return java.util.List.copyOf(providers.values());
    }
}
