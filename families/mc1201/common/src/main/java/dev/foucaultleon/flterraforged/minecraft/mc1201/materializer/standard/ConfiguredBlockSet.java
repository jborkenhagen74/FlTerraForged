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

    /**
     * Returns whether the user explicitly configured this block set.
     *
     * @return {@code true} for an explicit configuration override
     */
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
        double formation = NaturalMaterialField.sample(x, z, salt, 42.0D);
        double relief = Math.min(1.0D, Math.max(0.0D, sample.slope() / 2.5D));
        double stratified = Math.max(0.0D, Math.min(
                0.999999D,
                formation * 0.90D + relief * 0.07D + Math.floorMod(y, 7) * 0.003D));
        int index = Math.min(states.size() - 1, (int) Math.floor(stratified * states.size()));
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

    private record WeightedId(String id, int weight) {
    }
}
