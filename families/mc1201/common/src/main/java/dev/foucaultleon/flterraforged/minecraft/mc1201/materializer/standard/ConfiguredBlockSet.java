package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Immutable deterministic block palette parsed from materializer configuration. */
final class ConfiguredBlockSet {

    private static final int MAX_WEIGHT = 64;
    private static final int MAX_TOTAL_WEIGHT = 256;
    private static final int PATCH_SIZE = 3;

    private final List<BlockState> states;
    private final long salt;
    private final boolean configured;

    private ConfiguredBlockSet(List<BlockState> states, long salt, boolean configured) {
        this.states = List.copyOf(states);
        this.salt = salt;
        this.configured = configured;
    }

    /**
     * Parses a comma-separated block-id set while retaining supplied defaults when absent.
     *
     * @param options materializer options
     * @param key option key
     * @param defaults built-in fallback states
     * @return configured immutable set
     */
    static ConfiguredBlockSet parse(
            Map<String, String> options,
            String key,
            List<BlockState> defaults) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(key, "key");
        if (defaults == null || defaults.isEmpty()) {
            throw new IllegalArgumentException("defaults must not be empty for " + key);
        }
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return new ConfiguredBlockSet(defaults, key.hashCode(), false);
        }

        List<BlockState> parsed = new ArrayList<>();
        for (String entry : value.split(",")) {
            String token = entry.trim();
            if (token.isEmpty()) {
                continue;
            }
            WeightedId weighted = weightedId(token, key);
            Identifier id = identifier(weighted.id(), key);
            if (!Registries.BLOCK.containsId(id)) {
                throw new IllegalArgumentException(
                        "Unknown block id '" + weighted.id() + "' in materializer option '" + key + "'");
            }
            for (int copy = 0; copy < weighted.weight(); copy++) {
                parsed.add(Registries.BLOCK.get(id).getDefaultState());
                if (parsed.size() > MAX_TOTAL_WEIGHT) {
                    throw new IllegalArgumentException(
                            "Materializer option '" + key + "' exceeds total weight " + MAX_TOTAL_WEIGHT);
                }
            }
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Materializer option '" + key + "' must contain at least one block id");
        }
        return new ConfiguredBlockSet(parsed, key.hashCode(), true);
    }

    /** Returns whether the user explicitly configured this block set. */
    boolean isConfigured() {
        return configured;
    }

    /**
     * Selects one state deterministically from the sample's continuous worldgen signals.
     *
     * @param sample engine terrain sample
     * @return selected state
     */
    BlockState choose(TerrainSample sample) {
        return choose(sample, 0, (int) Math.floor(sample.surfaceHeight()), 0);
    }

    /**
     * Selects one state deterministically from world position and continuous Engine signals.
     *
     * @param sample engine terrain sample
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return selected state
     */
    BlockState choose(TerrainSample sample, int x, int y, int z) {
        Objects.requireNonNull(sample, "sample");
        if (states.size() == 1) {
            return states.get(0);
        }
        long hash = salt;
        int patchX = Math.floorDiv(x, PATCH_SIZE);
        int patchZ = Math.floorDiv(z, PATCH_SIZE);
        hash = mix(hash ^ (long) patchX * 0x9E3779B97F4A7C15L);
        hash = mix(hash ^ (long) y * 0x165667B19E3779F9L);
        hash = mix(hash ^ (long) patchZ * 0xC2B2AE3D27D4EB4FL);
        hash = mix(hash ^ quantize(sample.continentalness(), 16.0D));
        hash = mix(hash ^ quantize(sample.slope(), 16.0D));
        if (sample.climate().isAvailable()) {
            hash = mix(hash ^ quantize(sample.climate().temperature(), 16.0D));
            hash = mix(hash ^ quantize(sample.climate().moisture(), 16.0D));
        }
        if (sample.river().isAvailable()) {
            hash = mix(hash ^ quantize(sample.river().flow(), 4.0D));
            hash = mix(hash ^ quantize(sample.river().width(), 4.0D));
        }
        int index = Math.floorMod((int) (hash ^ (hash >>> 32)), states.size());
        return states.get(index);
    }

    private static WeightedId weightedId(String token, String key) {
        int separator = token.lastIndexOf('*');
        if (separator < 0) {
            return new WeightedId(token, 1);
        }
        String id = token.substring(0, separator).trim();
        String rawWeight = token.substring(separator + 1).trim();
        if (id.isEmpty() || rawWeight.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid weighted block '" + token + "' in materializer option '" + key + "'");
        }
        try {
            int weight = Integer.parseInt(rawWeight);
            if (weight < 1 || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException(
                        "Block weight in materializer option '" + key + "' must be in [1, "
                                + MAX_WEIGHT + "]");
            }
            return new WeightedId(id, weight);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid block weight '" + rawWeight + "' in materializer option '" + key + "'",
                    exception);
        }
    }

    private static Identifier identifier(String token, String key) {
        try {
            int separator = token.indexOf(':');
            return separator < 0
                    ? new Identifier("minecraft", token)
                    : new Identifier(token.substring(0, separator), token.substring(separator + 1));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid block id '" + token + "' in materializer option '" + key + "'",
                    exception);
        }
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return value;
    }

    private static long quantize(double value, double scale) {
        return Double.isFinite(value) ? Math.round(value * scale) : 0L;
    }

    private record WeightedId(String id, int weight) {
    }
}
