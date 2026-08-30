package dev.foucaultleon.flterraforged.engine.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Loader- and Minecraft-independent string configuration passed to an engine.
 *
 * <p>Engine implementations own the meaning of their keys. Structured config
 * files are parsed by the integration layer before values cross the API.</p>
 */
public final class EngineConfig {

    private static final EngineConfig EMPTY = new EngineConfig(Map.of());

    private final Map<String, String> values;

    private EngineConfig(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static EngineConfig empty() {
        return EMPTY;
    }

    public static EngineConfig of(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        return values.isEmpty() ? EMPTY : new EngineConfig(values);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public Map<String, String> asMap() {
        return values;
    }
}
