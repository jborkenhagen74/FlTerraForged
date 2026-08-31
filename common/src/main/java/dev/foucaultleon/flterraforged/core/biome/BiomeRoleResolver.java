package dev.foucaultleon.flterraforged.core.biome;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/**
 * Resolves a version-neutral biome role into a platform/version-native biome representation.
 *
 * <p>The shared climate router never imports Minecraft classes. Each Minecraft family implements
 * this interface using only the biomes that exist in that family. Newer families may therefore
 * map one semantic role to a richer set of native biomes without changing the Engine.</p>
 *
 * @param <T> native biome representation used by the version adapter
 */
@FunctionalInterface
public interface BiomeRoleResolver<T> {

    /**
     * Resolves a native biome for one semantic role and terrain sample.
     *
     * @param role version-neutral semantic biome role
     * @param sample Engine terrain/climate sample used for optional native sub-variants
     * @return native biome representation
     */
    T resolve(BiomeRole role, TerrainSample sample);
}
