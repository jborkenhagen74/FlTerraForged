package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Narrow delegation facade for vanilla 1.20.1 world-generation stages that remain Minecraft-owned.
 *
 * <p>The external engine owns continental shape, terrain, erosion, rivers and climate. Minecraft's
 * {@link NoiseChunkGenerator} supplies its native three-dimensional noise substrate, aquifers,
 * surface rules and mob population. Carving is intentionally absent from this facade in R46:
 * FlTerraForged owns cave/ravine masks and resolves their hydrology before block materialization.</p>
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

    /** Delegates vanilla biome mob population. */
    public void populateEntities(ChunkRegion region) {
        generator.populateEntities(region);
    }
}
