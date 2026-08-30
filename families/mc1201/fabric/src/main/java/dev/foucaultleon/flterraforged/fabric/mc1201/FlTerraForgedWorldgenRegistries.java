package dev.foucaultleon.flterraforged.fabric.mc1201;

import com.mojang.serialization.Codec;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.FlTerraForgedBiomeSource;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.FlTerraForgedChunkGenerator;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Registers the Minecraft 1.20.1 world-generation codecs during vanilla registry bootstrap.
 *
 * <p>The built-in biome-source and chunk-generator registries are frozen before Fabric's normal
 * {@code ModInitializer} entrypoint runs. Registration is therefore invoked from mixins targeting
 * {@code BiomeSources.registerAndGetDefault} and {@code ChunkGenerators.registerAndGetDefault},
 * while the registries are still mutable.</p>
 */
public final class FlTerraForgedWorldgenRegistries {

    /** Identifier of the FlTerraForged biome-source codec. */
    public static final Identifier BIOME_SOURCE_ID =
            new Identifier("flterraforged", "biome_source");

    /** Identifier of the FlTerraForged chunk-generator codec. */
    public static final Identifier CHUNK_GENERATOR_ID =
            new Identifier("flterraforged", "chunk_generator");

    private FlTerraForgedWorldgenRegistries() {
    }

    /**
     * Registers the biome-source codec during vanilla biome-source bootstrap.
     *
     * @param registry mutable vanilla biome-source codec registry
     */
    public static void registerBiomeSource(
            Registry<Codec<? extends BiomeSource>> registry) {
        if (!registry.containsId(BIOME_SOURCE_ID)) {
            Registry.register(registry, BIOME_SOURCE_ID, FlTerraForgedBiomeSource.CODEC);
        }
    }

    /**
     * Registers the chunk-generator codec during vanilla chunk-generator bootstrap.
     *
     * @param registry mutable vanilla chunk-generator codec registry
     */
    public static void registerChunkGenerator(
            Registry<Codec<? extends ChunkGenerator>> registry) {
        if (!registry.containsId(CHUNK_GENERATOR_ID)) {
            Registry.register(registry, CHUNK_GENERATOR_ID, FlTerraForgedChunkGenerator.CODEC);
        }
    }
}
