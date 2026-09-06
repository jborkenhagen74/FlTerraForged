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

/** Data-driven native biome palette used by the Minecraft 1.20.1 adapter. */
public final class BiomePalette implements BiomeRoleResolver<RegistryEntry<Biome>> {

    /** Fallback key used when a data pack omits a semantic role. */
    public static final String DEFAULT_KEY = "default";

    /** Codec used inside the custom biome-source configuration. */
    public static final Codec<BiomePalette> CODEC = Codec
            .unboundedMap(Codec.STRING, Biome.REGISTRY_CODEC.listOf())
            .xmap(BiomePalette::new, BiomePalette::entries);

    private static final int SPECIES_VARIATION_SCALE = 288;

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

    /** Returns the immutable serialized palette entries. */
    public Map<String, List<RegistryEntry<Biome>>> entries() {
        return entries;
    }

    /** {@inheritDoc} */
    @Override
    public RegistryEntry<Biome> resolve(BiomeRole role, TerrainSample sample) {
        return resolve(role, sample, 0, 0, 0L);
    }

    /**
     * Resolves a role using slow world-seeded spatial variation between native candidates.
     *
     * <p>Climate still controls the semantic role. The spatial field only chooses sub-variants,
     * preventing one smooth climate region from becoming hundreds of blocks of a single tree
     * species. Duplicate entries in a data palette intentionally act as deterministic weights.</p>
     *
     * @param role semantic biome role
     * @param sample Engine sample
     * @param blockX world block X
     * @param blockZ world block Z
     * @param seed world seed
     * @return selected native biome entry
     */
    public RegistryEntry<Biome> resolve(
            BiomeRole role,
            TerrainSample sample,
            int blockX,
            int blockZ,
            long seed) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(sample, "sample");
        List<RegistryEntry<Biome>> candidates = entries.get(role.key());
        if (candidates == null || candidates.isEmpty()) {
            candidates = entries.get(DEFAULT_KEY);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.get(variantIndex(role, sample, candidates.size(), blockX, blockZ, seed));
    }

    /** Returns the deterministic bootstrap fallback biome. */
    public RegistryEntry<Biome> fallback() {
        return entries.get(DEFAULT_KEY).get(0);
    }

    /** Returns all palette entries for {@link net.minecraft.world.biome.source.BiomeSource}. */
    public Stream<RegistryEntry<Biome>> stream() {
        return entries.values().stream().flatMap(List::stream).distinct();
    }

    private static int variantIndex(
            BiomeRole role,
            TerrainSample sample,
            int count,
            int blockX,
            int blockZ,
            long seed) {
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

        double ecological = temperature * 0.20D
                + moisture * 0.30D
                + continentalness * 0.18D
                + erosion * 0.12D
                + slope * 0.08D;
        double spatial = spatialSelector(blockX, blockZ, seed);
        double selector = ecological * 0.62D + spatial * 0.38D
                + role.ordinal() * 0.17320508075688773D;
        selector -= Math.floor(selector);
        return Math.min(count - 1, (int) Math.floor(selector * count));
    }

    private static double spatialSelector(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, SPECIES_VARIATION_SCALE);
        int cellZ = Math.floorDiv(z, SPECIES_VARIATION_SCALE);
        double tx = smooth(Math.floorMod(x, SPECIES_VARIATION_SCALE)
                / (double) SPECIES_VARIATION_SCALE);
        double tz = smooth(Math.floorMod(z, SPECIES_VARIATION_SCALE)
                / (double) SPECIES_VARIATION_SCALE);
        double a = hash01(cellX, cellZ, seed);
        double b = hash01(cellX + 1, cellZ, seed);
        double c = hash01(cellX, cellZ + 1, seed);
        double d = hash01(cellX + 1, cellZ + 1, seed);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private static double hash01(int x, int z, long seed) {
        long value = seed ^ 0xE7037ED1A0B428DBL;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0x165667B19E3779F9L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
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

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }
}
