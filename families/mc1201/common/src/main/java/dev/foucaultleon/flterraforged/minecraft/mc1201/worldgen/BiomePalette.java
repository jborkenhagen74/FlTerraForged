package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import com.mojang.serialization.Codec;
import dev.foucaultleon.flterraforged.core.biome.BiomeRole;
import dev.foucaultleon.flterraforged.core.biome.BiomeRoleResolver;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

/**
 * Data-driven native biome palette used by the Minecraft 1.20.1 adapter.
 *
 * <p>The serialized form is a map from stable {@link BiomeRole#key()} values to one or more native
 * biome registry entries. The schema therefore does not need a Java record component for every
 * biome Minecraft happens to expose. Later version families can reuse the same semantic roles and
 * supply larger candidate lists containing biomes that do not exist in 1.20.1.</p>
 */
public final class BiomePalette implements BiomeRoleResolver<RegistryEntry<Biome>> {

    /** Fallback key used when a data pack omits a semantic role. */
    public static final String DEFAULT_KEY = "default";

    /** Codec used inside the custom biome-source configuration. */
    public static final Codec<BiomePalette> CODEC = Codec
            .unboundedMap(Codec.STRING, Biome.REGISTRY_CODEC.listOf())
            .xmap(BiomePalette::new, BiomePalette::entries);

    private final Map<String, List<RegistryEntry<Biome>>> entries;

    /**
     * Creates and validates a configured native palette.
     *
     * @param entries role-key to native-biome candidate mappings
     */
    public BiomePalette(Map<String, List<RegistryEntry<Biome>>> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, List<RegistryEntry<Biome>>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, List<RegistryEntry<Biome>>> entry : entries.entrySet()) {
            String key = normalize(entry.getKey());
            List<RegistryEntry<Biome>> candidates = List.copyOf(
                    Objects.requireNonNull(entry.getValue(), "biome candidates for " + key));
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Biome palette entry '" + key + "' must not be empty");
            }
            if (normalized.putIfAbsent(key, candidates) != null) {
                throw new IllegalArgumentException("Duplicate biome palette key after normalization: " + key);
            }
        }
        if (!normalized.containsKey(DEFAULT_KEY)) {
            throw new IllegalArgumentException("Biome palette requires a '" + DEFAULT_KEY + "' fallback entry");
        }
        this.entries = Map.copyOf(normalized);
    }

    /**
     * Returns the immutable serialized palette entries.
     *
     * @return role-key mappings
     */
    public Map<String, List<RegistryEntry<Biome>>> entries() {
        return entries;
    }

    /**
     * Resolves a role to a 1.20.1 biome, selecting optional sub-variants from broad Engine signals.
     *
     * <p>Candidate lists are intentionally version-owned. A later adapter may provide more
     * candidates for the same role, while old adapters can keep a single candidate. Selection uses
     * broad climate/continental signals rather than block coordinates, preventing checkerboard
     * biome noise.</p>
     *
     * @param role semantic biome role
     * @param sample Engine sample
     * @return selected native biome entry
     */
    @Override
    public RegistryEntry<Biome> resolve(BiomeRole role, TerrainSample sample) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(sample, "sample");
        List<RegistryEntry<Biome>> candidates = entries.get(role.key());
        if (candidates == null || candidates.isEmpty()) {
            candidates = entries.get(DEFAULT_KEY);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.get(variantIndex(role, sample, candidates.size()));
    }

    /**
     * Returns the deterministic bootstrap fallback biome.
     *
     * @return first configured default biome
     */
    public RegistryEntry<Biome> fallback() {
        return entries.get(DEFAULT_KEY).get(0);
    }

    /** Returns all palette entries for {@link net.minecraft.world.biome.source.BiomeSource}. */
    public Stream<RegistryEntry<Biome>> stream() {
        return entries.values().stream().flatMap(List::stream).distinct();
    }

    private static int variantIndex(BiomeRole role, TerrainSample sample, int count) {
        double temperature = sample.climate().isAvailable() ? sample.climate().temperature() : 0.5D;
        double moisture = sample.climate().isAvailable() ? sample.climate().moisture() : 0.5D;
        double continentalness = sample.hasContinentalness()
                ? clamp01(sample.continentalness() * 0.5D + 0.5D)
                : 0.5D;
        double erosion = sample.hasErosion() ? clamp01(sample.erosion() * 0.5D + 0.5D) : 0.5D;
        double slope = sample.hasSlope() ? clamp01(sample.slope() / 3.0D) : 0.0D;

        if (role == BiomeRole.ALPINE_ROCK) {
            if (temperature < 0.28D) {
                return count - 1;
            }
            if (slope > 0.62D && count > 1) {
                return Math.min(1, count - 1);
            }
            return 0;
        }
        if (role == BiomeRole.OCEAN_COLD && count > 1) {
            return temperature < 0.16D ? count - 1 : 0;
        }
        if (role == BiomeRole.OCEAN_WARM && count > 1) {
            return temperature > 0.86D ? count - 1 : 0;
        }

        double selector = temperature * 0.24D
                + moisture * 0.34D
                + continentalness * 0.20D
                + erosion * 0.12D
                + slope * 0.10D;
        selector = selector + (role.ordinal() * 0.17320508075688773D);
        selector -= Math.floor(selector);
        return Math.min(count - 1, (int) Math.floor(selector * count));
    }

    private static String normalize(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Biome palette keys must not be blank");
        }
        return normalized;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
