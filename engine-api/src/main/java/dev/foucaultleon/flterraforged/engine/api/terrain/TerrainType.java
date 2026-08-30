package dev.foucaultleon.flterraforged.engine.api.terrain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Extensible semantic terrain identifier. It is intentionally not a Minecraft
 * ResourceLocation and can be defined by third-party engines.
 */
public record TerrainType(String namespace, String value) {

    private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

    public TerrainType {
        namespace = validate("namespace", namespace);
        value = validate("value", value);
    }

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

    @Override
    public String toString() {
        return namespace + ':' + value;
    }
}
