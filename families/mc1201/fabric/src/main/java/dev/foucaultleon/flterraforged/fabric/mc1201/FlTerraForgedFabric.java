package dev.foucaultleon.flterraforged.fabric.mc1201;

import dev.foucaultleon.flterraforged.fabric.mc1201.materializer.FabricMaterializerBootstrap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;

/** Fabric bootstrap for the Minecraft 1.20.1 reference binding. */
public final class FlTerraForgedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricMaterializerBootstrap.bootstrap();

        // The two codec registries are already frozen here in Minecraft 1.20.1.
        // Registration happens earlier through BiomeSourcesMixin and ChunkGeneratorsMixin.
        if (!Registries.BIOME_SOURCE.containsId(
                FlTerraForgedWorldgenRegistries.BIOME_SOURCE_ID)) {
            throw new IllegalStateException(
                    "FlTerraForged biome-source codec was not registered during registry bootstrap");
        }
        if (!Registries.CHUNK_GENERATOR.containsId(
                FlTerraForgedWorldgenRegistries.CHUNK_GENERATOR_ID)) {
            throw new IllegalStateException(
                    "FlTerraForged chunk-generator codec was not registered during registry bootstrap");
        }
    }
}
