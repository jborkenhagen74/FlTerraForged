package dev.foucaultleon.flterraforged.fabric.mc1201;

import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.FlTerraForgedBiomeSource;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.FlTerraForgedChunkGenerator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registers the two codecs required by the data-driven 1.20.1 world preset. */
public final class FlTerraForgedWorldgenRegistries {

    private static final String MOD_ID = "flterraforged";

    private FlTerraForgedWorldgenRegistries() {
    }

    /** Registers the custom biome-source and chunk-generator types. */
    public static void register() {
        Registry.register(
                Registries.BIOME_SOURCE,
                new Identifier(MOD_ID, "biome_source"),
                FlTerraForgedBiomeSource.CODEC);
        Registry.register(
                Registries.CHUNK_GENERATOR,
                new Identifier(MOD_ID, "chunk_generator"),
                FlTerraForgedChunkGenerator.CODEC);
    }
}
