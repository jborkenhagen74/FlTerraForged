package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.engine.api.chunk.ColumnSnapshot;
import dev.foucaultleon.flterraforged.engine.api.chunk.NaturalMaterial;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Default compatibility mapping from Engine natural materials to the established materializer SPI. */
public final class NaturalMaterialFallback {

    private NaturalMaterialFallback() {
    }

    /**
     * Resolves an Engine material through the established block-materializer methods.
     *
     * @param materializer active replaceable materializer
     * @param column immutable Engine column metadata
     * @param natural semantic natural material class
     * @param x absolute block X
     * @param y absolute block Y
     * @param z absolute block Z
     * @return concrete Minecraft block state
     */
    public static BlockState resolve(
            BlockMaterializer materializer,
            ColumnSnapshot column,
            NaturalMaterial natural,
            int x,
            int y,
            int z) {
        TerrainSample sample = column.terrain();
        return switch (natural) {
            case AIR -> materializer.airState(sample);
            case SURFACE -> materializer.composedTopState(sample, x, z);
            case SOIL -> materializer.fillerState(sample, x, y, z);
            case ROCK, DEEP_ROCK -> materializer.substrateState(sample);
            case BEDROCK -> materializer.bedrockState(sample);
            case WATER -> materializer.fluidState(sample);
            case LAVA -> Blocks.LAVA.getDefaultState();
        };
    }
}
