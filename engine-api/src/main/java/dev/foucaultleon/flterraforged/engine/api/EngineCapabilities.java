package dev.foucaultleon.flterraforged.engine.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable capability set advertised by an engine instance. */
public final class EngineCapabilities {

    private static final EngineCapabilities NONE = new EngineCapabilities(
            EnumSet.noneOf(EngineCapability.class));

    private final EnumSet<EngineCapability> values;

    private EngineCapabilities(EnumSet<EngineCapability> values) {
        this.values = values.clone();
    }

    /**
     * Returns an empty capability set.
     *
     * @return shared immutable empty capability set
     */
    public static EngineCapabilities none() {
        return NONE;
    }

    /**
     * Creates a capability set containing the supplied values.
     *
     * @param first first capability; must not be {@code null}
     * @param rest additional capabilities
     * @return immutable capability set
     */
    public static EngineCapabilities of(EngineCapability first, EngineCapability... rest) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(rest, "rest");
        EnumSet<EngineCapability> values = EnumSet.of(first, rest);
        return new EngineCapabilities(values);
    }

    /**
     * Tests whether this set advertises a capability.
     *
     * @param capability capability to test
     * @return {@code true} when the capability is present
     */
    public boolean supports(EngineCapability capability) {
        return values.contains(Objects.requireNonNull(capability, "capability"));
    }

    /**
     * Returns an unmodifiable view of the advertised capabilities.
     *
     * @return immutable set view
     */
    public Set<EngineCapability> asSet() {
        return Collections.unmodifiableSet(values);
    }

    /**
     * Returns a human-readable representation of the capability set.
     *
     * @return capability set string
     */
    @Override
    public String toString() {
        return values.toString();
    }
}
