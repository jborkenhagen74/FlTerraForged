package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;

/** Minecraft-family facade for shared dry-riparian predicates. */
public final class RiparianZone {

    private RiparianZone() {
    }

    /**
     * Returns whether the sample is a dry-climate riverbank rather than the wet channel itself.
     *
     * @param sample Engine terrain sample
     * @return {@code true} when the sample belongs to the dry-climate riparian fringe
     */
    public static boolean isDryBank(TerrainSample sample) {
        return BiomeClimateRouter.isDryRiparianBank(sample);
    }
}
