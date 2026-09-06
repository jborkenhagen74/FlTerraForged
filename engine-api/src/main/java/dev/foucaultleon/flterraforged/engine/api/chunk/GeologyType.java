package dev.foucaultleon.flterraforged.engine.api.chunk;

/**
 * Broad Minecraft-neutral geology class for one natural terrain column.
 *
 * <p>The classification is intentionally semantic rather than block-specific. Materializers may
 * map the same geology to different block palettes.</p>
 */
public enum GeologyType {
    /** Mixed or unavailable geology. */
    MIXED,
    /** Sedimentary geology such as sandstone, shale or conglomerate families. */
    SEDIMENTARY,
    /** Carbonate-rich geology such as limestone families. */
    CARBONATE,
    /** Granitic intrusive geology. */
    GRANITIC,
    /** Metamorphic geology. */
    METAMORPHIC,
    /** Volcanic or basaltic geology. */
    VOLCANIC
}
