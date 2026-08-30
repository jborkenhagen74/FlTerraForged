package dev.foucaultleon.flterraforged.engine.api;

/**
 * Immutable world context passed to an external engine.
 *
 * @param seed deterministic world seed
 * @param minY inclusive minimum build height
 * @param maxYExclusive exclusive maximum build height
 * @param seaLevel sea level in block coordinates
 */
public record EngineContext(long seed, int minY, int maxYExclusive, int seaLevel) {

    /**
     * Creates and validates a world context.
     *
     * @throws IllegalArgumentException if the world bounds are invalid or sea level lies outside them
     */
    public EngineContext {
        if (maxYExclusive <= minY) {
            throw new IllegalArgumentException("maxYExclusive must be greater than minY");
        }
        if (seaLevel < minY || seaLevel >= maxYExclusive) {
            throw new IllegalArgumentException("seaLevel must be inside world bounds");
        }
    }

    /**
     * Returns the total vertical size of the world.
     *
     * @return number of vertical block positions
     */
    public int height() {
        return maxYExclusive - minY;
    }
}
