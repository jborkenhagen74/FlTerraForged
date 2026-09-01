package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.core.biome.BiomeRole;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

/** Maps version-neutral climate roles to the native Minecraft 1.20.1 biome palette. */
public final class NativeBiomeRouter {

    private NativeBiomeRouter() {
    }

    /**
     * Resolves a Minecraft biome from continuous terrain and climate signals.
     *
     * <p>The shared router selects a version-neutral role. This adapter only maps that role to a
     * biome available in Minecraft 1.20.1. Later version families may map the same roles to newer
     * or more specific vanilla biomes without changing Engine climate logic.</p>
     *
     * @param sample external-engine terrain sample
     * @param palette configured native biome palette
     * @param seaLevel active Minecraft sea level
     * @return selected Minecraft biome entry
     */
    public static RegistryEntry<Biome> route(
            TerrainSample sample,
            BiomePalette palette,
            int seaLevel) {
        BiomeRole role = BiomeClimateRouter.route(sample);
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                && seaLevel - sample.surfaceHeight() >= 12.0D) {
            role = switch (role) {
                case OCEAN_COLD -> BiomeRole.OCEAN_DEEP_COLD;
                case OCEAN_WARM -> BiomeRole.OCEAN_DEEP_WARM;
                default -> BiomeRole.OCEAN_DEEP_TEMPERATE;
            };
        }
        return palette.resolve(role, sample);
    }
}
