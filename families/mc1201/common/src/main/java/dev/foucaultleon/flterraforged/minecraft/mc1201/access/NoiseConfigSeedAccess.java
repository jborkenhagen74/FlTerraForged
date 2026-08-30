package dev.foucaultleon.flterraforged.minecraft.mc1201.access;

/** Exposes the world seed captured from Minecraft 1.20.1 {@code NoiseConfig}. */
public interface NoiseConfigSeedAccess {

    /**
     * Returns the seed used to create the noise configuration.
     *
     * @return world-generation seed
     */
    long flterraforged$getSeed();
}
