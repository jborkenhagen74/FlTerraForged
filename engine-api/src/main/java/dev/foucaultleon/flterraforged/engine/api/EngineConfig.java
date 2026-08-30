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

    /**
     * Returns an empty configuration.
     *
     * @return shared immutable empty configuration
     */
    public static EngineConfig empty() {
        return EMPTY;
    }

    /**
     * Creates an immutable configuration from string key/value pairs.
     *
     * @param values configuration values
     * @return immutable configuration
     */
    public static EngineConfig of(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        return values.isEmpty() ? EMPTY : new EngineConfig(values);
    }

    /**
     * Looks up a configuration value.
     *
     * @param key configuration key
     * @return optional value associated with the key
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(Objects.requireNonNull(key, "key")));
    }

    /**
     * Looks up a configuration value with a fallback.
     *
     * @param key configuration key
     * @param defaultValue value returned when the key is absent
     * @return configured value or {@code defaultValue}
     */
    public String getOrDefault(String key, String defaultValue) {
        return values.getOrDefault(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(defaultValue, "defaultValue"));
    }

    /**
     * Returns the immutable configuration map.
     *
     * @return immutable configuration values
     */
    public Map<String, String> asMap() {
        return values;
    }
}
