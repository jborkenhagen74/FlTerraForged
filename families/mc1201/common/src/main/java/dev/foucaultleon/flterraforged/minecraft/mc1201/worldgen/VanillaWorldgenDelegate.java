package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Narrow delegation facade for vanilla 1.20.1 world-generation stages that
 * should remain Minecraft-owned.
 *
 * <p>The external engine owns continental shape, terrain, erosion, rivers and
 * climate. Minecraft's {@link NoiseChunkGenerator} remains responsible for its
 * native three-dimensional noise substrate, aquifers, surface rules, carvers
 * and mob population. The host may apply narrow semantic repairs after a delegated stage when
 * vanilla cannot know about Engine-owned hydrology.</p>
 */
public final class VanillaWorldgenDelegate {

    private final NoiseChunkGenerator generator;

    /** Creates a vanilla delegate using the exact biome source and settings. */
    public VanillaWorldgenDelegate(
            BiomeSource biomeSource,
            RegistryEntry<ChunkGeneratorSettings> settings) {
        this.generator = new NoiseChunkGenerator(biomeSource, settings);
    }

    /** Runs Minecraft's NoiseRouter density fill including aquifers. */
    public CompletableFuture<Chunk> populateNoise(
            Executor executor,
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structures,
            Chunk chunk) {
        return generator.populateNoise(executor, blender, noiseConfig, structures, chunk);
    }

    /** Applies the configured vanilla surface-rule graph. */
    public void buildSurface(
            ChunkRegion region,
            StructureAccessor structures,
            NoiseConfig noiseConfig,
            Chunk chunk) {
        generator.buildSurface(region, structures, noiseConfig, chunk);
    }

    /** Runs the configured vanilla AIR/LIQUID carver step. */
    public void carve(
            ChunkRegion region,
            long seed,
            NoiseConfig noiseConfig,
            BiomeAccess biomeAccess,
            StructureAccessor structures,
            Chunk chunk,
            GenerationStep.Carver step) {
        generator.carve(region, seed, noiseConfig, biomeAccess, structures, chunk, step);
    }

    /** Delegates vanilla biome mob population. */
    public void populateEntities(ChunkRegion region) {
        generator.populateEntities(region);
    }
}
