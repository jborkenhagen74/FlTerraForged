package dev.foucaultleon.flterraforged.engine.api.chunk;

/**
 * Immutable, thread-safe natural-world snapshot for one 16 by 16 Minecraft-compatible chunk.
 *
 * <p>A snapshot is the ownership boundary between the Minecraft-neutral Engine and a host game
 * adapter. The Engine must completely resolve terrain, hydrology, geology, caves, underground
 * fluids and the world floor before publishing the snapshot. Consumers must never cause the
 * snapshot to call back into a chunk generator or a higher world-generation stage.</p>
 */
public interface ChunkSnapshot {

    /** Width and depth of a snapshot in columns. */
    int WIDTH = 16;

    /**
     * Returns the X coordinate of this chunk.
     *
     * @return chunk X
     */
    int chunkX();

    /**
     * Returns the Z coordinate of this chunk.
     *
     * @return chunk Z
     */
    int chunkZ();

    /**
     * Returns the inclusive minimum Y represented by this snapshot.
     *
     * @return minimum Y
     */
    int minY();

    /**
     * Returns the exclusive maximum Y represented by this snapshot.
     *
     * @return exclusive maximum Y
     */
    int maxYExclusive();

    /**
     * Returns immutable metadata for a local column.
     *
     * @param localX local X in the range 0..15
     * @param localZ local Z in the range 0..15
     * @return column metadata
     * @throws IndexOutOfBoundsException when a local coordinate is outside the snapshot
     */
    ColumnSnapshot column(int localX, int localZ);

    /**
     * Returns the Engine-owned natural material at a local block coordinate.
     *
     * @param localX local X in the range 0..15
     * @param y absolute world Y
     * @param localZ local Z in the range 0..15
     * @return natural material class
     * @throws IndexOutOfBoundsException when the coordinate is outside the snapshot
     */
    NaturalMaterial materialAt(int localX, int y, int localZ);

    /**
     * Returns the represented vertical size.
     *
     * @return number of represented Y positions
     */
    default int height() {
        return maxYExclusive() - minY();
    }
}
