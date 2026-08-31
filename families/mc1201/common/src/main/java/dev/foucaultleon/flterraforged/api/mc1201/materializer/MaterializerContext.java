package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import java.util.Map;
import java.util.Objects;
import net.minecraft.block.BlockState;

/**
 * Immutable Minecraft generation context supplied to a materializer provider.
 *
 * @param minY minimum build Y of the active generation shape
 * @param maxYExclusive exclusive maximum build Y of the active generation shape
 * @param seaLevel configured vanilla sea level
 * @param defaultBlock default solid substrate from the active chunk-generator settings
 * @param defaultFluid default fluid from the active chunk-generator settings
 * @param options immutable provider options loaded from the materializer configuration
 */
public record MaterializerContext(
        int minY,
        int maxYExclusive,
        int seaLevel,
        BlockState defaultBlock,
        BlockState defaultFluid,
        Map<String, String> options) {

    /**
     * Validates and freezes one materializer context.
     *
     * @param minY minimum build Y of the active generation shape
     * @param maxYExclusive exclusive maximum build Y of the active generation shape
     * @param seaLevel configured vanilla sea level
     * @param defaultBlock default solid substrate from the active chunk-generator settings
     * @param defaultFluid default fluid from the active chunk-generator settings
     * @param options immutable provider options
     */
    public MaterializerContext {
        if (maxYExclusive <= minY + 2) {
            throw new IllegalArgumentException("maxYExclusive must be above minY by at least 3 blocks");
        }
        Objects.requireNonNull(defaultBlock, "defaultBlock");
        Objects.requireNonNull(defaultFluid, "defaultFluid");
        options = Map.copyOf(Objects.requireNonNull(options, "options"));
    }

    /**
     * Backwards-compatible constructor for providers that do not consume configuration options.
     *
     * @param minY minimum build Y
     * @param maxYExclusive exclusive maximum build Y
     * @param seaLevel configured sea level
     * @param defaultBlock default solid block
     * @param defaultFluid default fluid block
     */
    public MaterializerContext(
            int minY,
            int maxYExclusive,
            int seaLevel,
            BlockState defaultBlock,
            BlockState defaultFluid) {
        this(minY, maxYExclusive, seaLevel, defaultBlock, defaultFluid, Map.of());
    }

    /**
     * Returns an option value or a fallback when it is absent or blank.
     *
     * @param key option key
     * @param fallback fallback value
     * @return configured value or fallback
     */
    public String option(String key, String fallback) {
        String value = options.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
