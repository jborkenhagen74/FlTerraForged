package dev.foucaultleon.flterraforged.engine.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Globally unique identifier of an engine provider.
 *
 * @param namespace provider namespace
 * @param value provider-specific identifier
 */
public record EngineId(String namespace, String value) {

    private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

    /**
     * Creates and validates an engine identifier.
     *
     * @throws NullPointerException if a component is {@code null}
     * @throws IllegalArgumentException if a component contains unsupported characters
     */
    public EngineId {
        namespace = validate("namespace", namespace);
        value = validate("value", value);
    }

    /**
     * Creates an engine identifier from namespace and value.
     *
     * @param namespace provider namespace
     * @param value provider-specific identifier
     * @return validated engine identifier
     */
    public static EngineId of(String namespace, String value) {
        return new EngineId(namespace, value);
    }

    /**
     * Parses an identifier in {@code namespace:value} form.
     *
     * @param text textual identifier
     * @return parsed engine identifier
     * @throws IllegalArgumentException if the text is not a valid engine identifier
     */
    public static EngineId parse(String text) {
        Objects.requireNonNull(text, "text");
        int separator = text.indexOf(':');
        if (separator <= 0 || separator == text.length() - 1 || text.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Engine id must be '<namespace>:<value>': " + text);
        }
        return new EngineId(text.substring(0, separator), text.substring(separator + 1));
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
     * @return canonical engine identifier
     */
    @Override
    public String toString() {
        return namespace + ':' + value;
    }
}
