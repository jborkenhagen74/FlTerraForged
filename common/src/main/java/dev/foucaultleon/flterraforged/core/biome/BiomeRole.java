package dev.foucaultleon.flterraforged.core.biome;

import java.util.Locale;

/**
 * Minecraft-version-neutral biome roles selected from Engine climate and terrain semantics.
 *
 * <p>Version adapters map these roles to the native biomes available in their target Minecraft
 * version. Several roles may intentionally map to the same biome in an older version while newer
 * adapters can use additional native biomes without changing the Engine or climate presets.</p>
 */
public enum BiomeRole {
    /** Cold or polar ocean water. */
    OCEAN_COLD,
    /** Temperate ocean water. */
    OCEAN_TEMPERATE,
    /** Warm shallow ocean water. */
    OCEAN_WARM,
    /** Cold deep-ocean water. */
    OCEAN_DEEP_COLD,
    /** Temperate deep-ocean water. */
    OCEAN_DEEP_TEMPERATE,
    /** Warm deep-ocean water. */
    OCEAN_DEEP_WARM,
    /** Sandy or otherwise soft coast. */
    COAST_SANDY,
    /** Exposed rocky coast. */
    COAST_ROCKY,
    /** Cold river or lake water. */
    RIVER_COLD,
    /** Temperate/warm river or lake water. */
    RIVER_TEMPERATE,
    /** Treeless polar or sub-polar plain. */
    POLAR_PLAIN,
    /** Boreal conifer forest. */
    BOREAL_FOREST,
    /** Cool open grassland. */
    COOL_GRASSLAND,
    /** Cool deciduous or mixed forest. */
    COOL_FOREST,
    /** Temperate open grassland. */
    TEMPERATE_GRASSLAND,
    /** Temperate open woodland. */
    TEMPERATE_OPEN_WOODLAND,
    /** Temperate forest. */
    TEMPERATE_FOREST,
    /** Dense temperate forest. */
    TEMPERATE_DENSE_FOREST,
    /** Low-gradient wetland. */
    WETLAND,
    /** High-elevation meadow. */
    ALPINE_MEADOW,
    /** High-elevation exposed rock or windswept land. */
    ALPINE_ROCK,
    /** Warm dry grassland resembling a Mediterranean open landscape. */
    MEDITERRANEAN_GRASSLAND,
    /** Warm moderately dry woodland resembling a Mediterranean forest landscape. */
    MEDITERRANEAN_WOODLAND,
    /** Very hot and very dry climate. */
    HOT_DRY,
    /** Hot seasonal climate with a pronounced dry component. */
    HOT_SEASONAL,
    /** Hot and very humid climate. */
    HOT_WET;

    /**
     * Returns the stable lowercase configuration key for this semantic role.
     *
     * @return lowercase role key
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

