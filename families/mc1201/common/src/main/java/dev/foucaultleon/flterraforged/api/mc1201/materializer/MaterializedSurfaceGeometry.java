package dev.foucaultleon.flterraforged.api.mc1201.materializer;

/**
 * Immutable physical top-surface geometry reported by a block materializer.
 *
 * <p>Minecraft still stores the surface state in one integer block cell, but providers such as
 * layered or partial-block terrain packs may occupy only part of that cell. {@code topY} is the
 * continuous world-space top of the solid geometry and therefore lets world-generation placement
 * and water-depth checks avoid assuming every materialized surface is exactly one block high.</p>
 *
 * @param blockY Y coordinate of the Minecraft block cell containing the top material
 * @param topY continuous world-space top of the occupied solid geometry
 * @param supportsDryPlacement whether structures may treat the reported top as a dry support plane
 */
public record MaterializedSurfaceGeometry(
        int blockY,
        double topY,
        boolean supportsDryPlacement) {

    /**
     * Validates one materialized surface description.
     *
     * @param blockY Y coordinate of the containing block cell
     * @param topY continuous world-space top of the solid geometry
     * @param supportsDryPlacement whether the top may support dry placement
     */
    public MaterializedSurfaceGeometry {
        if (!Double.isFinite(topY)) {
            throw new IllegalArgumentException("topY must be finite");
        }
        if (topY < blockY || topY > blockY + 1.0D) {
            throw new IllegalArgumentException("topY must lie inside the reported block cell");
        }
    }

    /**
     * Creates the geometry for a conventional full block.
     *
     * @param blockY topmost solid block Y
     * @return full-block geometry ending at {@code blockY + 1}
     */
    public static MaterializedSurfaceGeometry fullBlock(int blockY) {
        return new MaterializedSurfaceGeometry(blockY, blockY + 1.0D, true);
    }

    /**
     * Returns the occupied height inside the containing Minecraft block cell.
     *
     * @return occupied height in block units
     */
    public double occupiedHeight() {
        return topY - blockY;
    }
}
