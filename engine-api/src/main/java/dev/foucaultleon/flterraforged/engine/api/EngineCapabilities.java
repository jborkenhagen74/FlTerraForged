package dev.foucaultleon.flterraforged.engine.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable capability set advertised by an engine instance. */
public final class EngineCapabilities {

    private static final EngineCapabilities NONE = new EngineCapabilities(
            EnumSet.noneOf(EngineCapability.class));

    private final EnumSet<EngineCapability> values;

    private EngineCapabilities(EnumSet<EngineCapability> values) {
        this.values = values.clone();
    }

    public static EngineCapabilities none() {
        return NONE;
    }

    public static EngineCapabilities of(EngineCapability first, EngineCapability... rest) {
        EnumSet<EngineCapability> values = EnumSet.of(first, rest);
        return new EngineCapabilities(values);
    }

    public boolean supports(EngineCapability capability) {
        return values.contains(capability);
    }

    public Set<EngineCapability> asSet() {
        return Collections.unmodifiableSet(values);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
