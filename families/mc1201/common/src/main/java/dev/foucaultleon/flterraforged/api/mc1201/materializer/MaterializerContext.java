package dev.foucaultleon.flterraforged.api.mc1201.materializer;

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
 */
public record MaterializerContext(
        int minY,
        int maxYExclusive,
        int seaLevel,
        BlockState defaultBlock,
        BlockState defaultFluid) {

    /**
     * Validates and freezes one materializer context.
     *
     * @param minY minimum build Y of the active generation shape
     * @param maxYExclusive exclusive maximum build Y of the active generation shape
     * @param seaLevel configured vanilla sea level
     * @param defaultBlock default solid substrate from the active chunk-generator settings
     * @param defaultFluid default fluid from the active chunk-generator settings
     */
    public MaterializerContext {
        if (maxYExclusive <= minY + 2) {
            throw new IllegalArgumentException("maxYExclusive must be above minY by at least 3 blocks");
        }
        Objects.requireNonNull(defaultBlock, "defaultBlock");
        Objects.requireNonNull(defaultFluid, "defaultFluid");
    }
}
