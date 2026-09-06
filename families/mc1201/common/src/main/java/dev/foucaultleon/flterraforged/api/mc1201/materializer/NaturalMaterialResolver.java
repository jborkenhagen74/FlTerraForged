package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.chunk.ColumnSnapshot;
import dev.foucaultleon.flterraforged.engine.api.chunk.NaturalMaterial;
import net.minecraft.block.BlockState;

/**
 * Optional materializer extension for direct Engine-owned natural chunk materialization.
 *
 * <p>A {@link BlockMaterializer} may additionally implement this interface when it needs custom
 * geology-aware mappings for caves, deep rock, fluids or partial-height surface blocks. Existing
 * materializers remain binary/source compatible because the Minecraft adapter provides a fallback
 * mapping through the established {@link BlockMaterializer} methods.</p>
 */
public interface NaturalMaterialResolver {

    /**
     * Resolves one Engine-owned natural material cell to a concrete Minecraft block state.
     *
     * @param column immutable Engine column metadata, including fractional terrain and geology
     * @param material semantic natural material class
     * @param x absolute block X
     * @param y absolute block Y
     * @param z absolute block Z
     * @return concrete block state to place
     */
    BlockState resolveNaturalMaterial(
            ColumnSnapshot column,
            NaturalMaterial material,
            int x,
            int y,
            int z);
}
