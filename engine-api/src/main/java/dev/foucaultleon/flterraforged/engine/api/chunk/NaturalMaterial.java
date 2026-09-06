package dev.foucaultleon.flterraforged.engine.api.chunk;

/**
 * Minecraft-neutral natural material class produced by the Engine.
 *
 * <p>The Engine owns the geometry and the semantic material class. A host-side block materializer
 * maps these values to concrete game blocks, allowing providers such as Conquest Reforged to use
 * their own palettes and variable-height blocks without leaking Minecraft types into the Engine.</p>
 */
public enum NaturalMaterial {
    /** Empty space produced by terrain shape or a cave. */
    AIR,
    /** Visible top material at a dry natural surface. */
    SURFACE,
    /** Near-surface soil or loose filler. */
    SOIL,
    /** Ordinary structural rock. */
    ROCK,
    /** Deep, pressure-dominated structural rock. */
    DEEP_ROCK,
    /** Protected world-floor material. */
    BEDROCK,
    /** Natural water, including oceans, rivers, lakes and aquifers. */
    WATER,
    /** Natural deep lava. */
    LAVA
}
