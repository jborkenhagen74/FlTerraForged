package dev.foucaultleon.flterraforged.engine.api.terrain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Extensible semantic terrain identifier.
 *
 * <p>It is intentionally not a Minecraft resource identifier and can be defined
 * by third-party engines.</p>
 *
 * @param namespace terrain namespace
 * @param value terrain identifier within the namespace
 */
public record TerrainType(String namespace, String value) {

    private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

    /**
     * Creates and validates a terrain type.
     *
     * @param namespace terrain namespace
     * @param value terrain identifier within the namespace
     * @throws NullPointerException if a component is {@code null}
     * @throws IllegalArgumentException if a component contains unsupported characters
     */
    public TerrainType {
        namespace = validate("namespace", namespace);
        value = validate("value", value);
    }

    /**
     * Creates a semantic terrain type.
     *
     * @param namespace terrain namespace
     * @param value terrain identifier within the namespace
     * @return validated terrain type
     */
    public static TerrainType of(String namespace, String value) {
        return new TerrainType(namespace, value);
    }

    private static String validate(String field, String text) {
        Objects.requireNonNull(text, field);
        if (!PART.matcher(text).matches()) {
            throw new IllegalArgumentException(
                    field + " must match " + PART.pattern() + ": " + text);
        }
        return text;
    }

    /**
     * Returns the canonical {@code namespace:value} representation.
     *
     * @return canonical terrain identifier
     */
    @Override
    public String toString() {
        return namespace + ':' + value;
    }
}
