package dev.foucaultleon.flterraforged.api.mc1201.materializer;

/**
 * Immutable capability description for a Minecraft 1.20.1 block materializer.
 *
 * @param verticalResolution smallest supported vertical geometry step in block units
 * @param partialBlocks whether non-full-block terrain shapes can be emitted
 * @param waterlogging whether emitted terrain shapes can carry water through waterlogging
 */
public record MaterializerCapabilities(
        double verticalResolution,
        boolean partialBlocks,
        boolean waterlogging) {

    /**
     * Validates the advertised materializer capabilities.
     *
     * @param verticalResolution smallest supported vertical geometry step in block units
     * @param partialBlocks whether non-full-block terrain shapes can be emitted
     * @param waterlogging whether emitted terrain shapes can carry water through waterlogging
     */
    public MaterializerCapabilities {
        if (!Double.isFinite(verticalResolution) || verticalResolution <= 0.0D) {
            throw new IllegalArgumentException("verticalResolution must be finite and > 0");
        }
    }
}
