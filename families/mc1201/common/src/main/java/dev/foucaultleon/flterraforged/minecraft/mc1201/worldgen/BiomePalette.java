package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

/** Vanilla/native biome palette used by the first Minecraft 1.20.1 adapter. */
public record BiomePalette(
        RegistryEntry<Biome> ocean,
        RegistryEntry<Biome> coast,
        RegistryEntry<Biome> river,
        RegistryEntry<Biome> plains,
        RegistryEntry<Biome> forest,
        RegistryEntry<Biome> desert,
        RegistryEntry<Biome> jungle,
        RegistryEntry<Biome> mountains,
        RegistryEntry<Biome> snowy) {

    /** Codec used inside the custom biome-source configuration. */
    public static final Codec<BiomePalette> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.REGISTRY_CODEC.fieldOf("ocean").forGetter(BiomePalette::ocean),
            Biome.REGISTRY_CODEC.fieldOf("coast").forGetter(BiomePalette::coast),
            Biome.REGISTRY_CODEC.fieldOf("river").forGetter(BiomePalette::river),
            Biome.REGISTRY_CODEC.fieldOf("plains").forGetter(BiomePalette::plains),
            Biome.REGISTRY_CODEC.fieldOf("forest").forGetter(BiomePalette::forest),
            Biome.REGISTRY_CODEC.fieldOf("desert").forGetter(BiomePalette::desert),
            Biome.REGISTRY_CODEC.fieldOf("jungle").forGetter(BiomePalette::jungle),
            Biome.REGISTRY_CODEC.fieldOf("mountains").forGetter(BiomePalette::mountains),
            Biome.REGISTRY_CODEC.fieldOf("snowy").forGetter(BiomePalette::snowy)
    ).apply(instance, BiomePalette::new));

    /** Returns all palette entries for {@link net.minecraft.world.biome.source.BiomeSource}. */
    public Stream<RegistryEntry<Biome>> stream() {
        return Stream.of(ocean, coast, river, plains, forest, desert, jungle, mountains, snowy)
                .distinct();
    }
}
