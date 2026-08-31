package example.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.DelegatingBlockMaterializer;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Example decorator that changes only hydrology bed material. */
public final class ExampleMaterializer extends DelegatingBlockMaterializer {

    /**
     * Creates the example decorator.
     *
     * @param delegate standard or custom base materializer
     */
    public ExampleMaterializer(BlockMaterializer delegate) {
        super(delegate);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample) {
        return Blocks.COBBLESTONE.getDefaultState();
    }
}
