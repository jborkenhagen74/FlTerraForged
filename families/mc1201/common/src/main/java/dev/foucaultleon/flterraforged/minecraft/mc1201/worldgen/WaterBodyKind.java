package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

/**
 * Provider-resolved local water-body class used by Minecraft-side environment decisions.
 *
 * <p>This classification deliberately lives outside the Engine. The Engine supplies continuous
 * terrain and hydrology semantics; FlTerraForged combines them with the selected materializer's
 * physical X/Z geometry before deciding whether a location is a narrow channel or genuinely open
 * marine water.</p>
 */
enum WaterBodyKind {
    /** No material water can be represented at the sampled center. */
    DRY,
    /** A semantic river owns the material water column. */
    RIVER,
    /** A semantic lake or lake-edge water column owns the material water. */
    LAKE,
    /** Marine semantics exist, but provider-resolved surrounding water is too confined. */
    CONFINED_CHANNEL,
    /** Provider-resolved surrounding water proves a broad open marine environment. */
    OPEN_MARINE
}
