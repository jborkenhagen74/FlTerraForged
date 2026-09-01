package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

/** Minecraft 1.20.1 biome source backed by the external terrain engine. */
public final class FlTerraForgedBiomeSource extends BiomeSource {

    /** Codec registered in the vanilla biome-source type registry. */
    public static final Codec<FlTerraForgedBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomePalette.CODEC.fieldOf("palette").forGetter(FlTerraForgedBiomeSource::palette)
            ).apply(instance, FlTerraForgedBiomeSource::new));

    private final BiomePalette palette;
    private volatile TerrainWorld terrainWorld;
    private volatile int seaLevel = 63;

    /** Creates an unbound biome source from a native biome palette. */
    public FlTerraForgedBiomeSource(BiomePalette palette) {
        this.palette = Objects.requireNonNull(palette, "palette");
    }

    /**
     * Binds this biome source to the same engine world used by the chunk generator.
     *
     * @param terrainWorld active engine world
     * @param seaLevel active Minecraft sea level used for shallow/deep ocean roles
     */
    public void bind(TerrainWorld terrainWorld, int seaLevel) {
        this.terrainWorld = Objects.requireNonNull(terrainWorld, "terrainWorld");
        this.seaLevel = seaLevel;
    }

    /**
     * Compatibility overload using the vanilla Overworld sea level.
     *
     * @param terrainWorld active engine world
     */
    public void bind(TerrainWorld terrainWorld) {
        bind(terrainWorld, 63);
    }

    /** Returns the serialized palette. */
    public BiomePalette palette() {
        return palette;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return palette.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(
            int biomeX,
            int biomeY,
            int biomeZ,
            MultiNoiseUtil.MultiNoiseSampler noise) {
        TerrainWorld world = terrainWorld;
        if (world == null) {
            // Registry/bootstrap code can query a biome source before NoiseConfig
            // exists. Use a deterministic safe fallback until the generator binds it.
            return palette.fallback();
        }
        int blockX = BiomeCoords.toBlock(biomeX);
        int blockZ = BiomeCoords.toBlock(biomeZ);
        TerrainSample sample = world.sample(blockX, blockZ);
        return NativeBiomeRouter.route(sample, palette, seaLevel);
    }
}
