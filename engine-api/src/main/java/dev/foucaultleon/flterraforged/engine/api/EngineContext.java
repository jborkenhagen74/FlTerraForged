package dev.foucaultleon.flterraforged.engine.api;

/** Immutable world context passed to an external engine. */
public record EngineContext(long seed, int minY, int maxYExclusive, int seaLevel) {

    public EngineContext {
        if (maxYExclusive <= minY) {
            throw new IllegalArgumentException("maxYExclusive must be greater than minY");
        }
        if (seaLevel < minY || seaLevel >= maxYExclusive) {
            throw new IllegalArgumentException("seaLevel must be inside world bounds");
        }
    }

    public int height() {
        return maxYExclusive - minY;
    }
}
