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
            Identifier id = identifier(token, key);
            if (!Registries.BLOCK.containsId(id)) {
                throw new IllegalArgumentException(
                        "Unknown block id '" + token + "' in materializer option '" + key + "'");
            }
            parsed.add(Registries.BLOCK.get(id).getDefaultState());
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
        Objects.requireNonNull(sample, "sample");
        if (states.size() == 1) {
            return states.get(0);
        }
        long hash = salt;
        hash = mix(hash ^ Double.doubleToLongBits(sample.surfaceHeight()));
        hash = mix(hash ^ Double.doubleToLongBits(sample.continentalness()));
        hash = mix(hash ^ Double.doubleToLongBits(sample.slope()));
        if (sample.climate().isAvailable()) {
            hash = mix(hash ^ Double.doubleToLongBits(sample.climate().temperature()));
            hash = mix(hash ^ Double.doubleToLongBits(sample.climate().moisture()));
        }
        if (sample.river().isAvailable()) {
            hash = mix(hash ^ Double.doubleToLongBits(sample.river().flow()));
            hash = mix(hash ^ Double.doubleToLongBits(sample.river().width()));
        }
        int index = Math.floorMod((int) (hash ^ (hash >>> 32)), states.size());
        return states.get(index);
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
}
