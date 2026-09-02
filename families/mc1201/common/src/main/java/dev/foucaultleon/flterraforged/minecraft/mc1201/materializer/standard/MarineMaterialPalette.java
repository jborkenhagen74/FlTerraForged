package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Natural depth-, slope- and climate-aware full-block formations for marine floors. */
final class MarineMaterialPalette {

    private final ConfiguredBlockSet shallowWarm;
    private final ConfiguredBlockSet shallowCold;
    private final ConfiguredBlockSet shelf;
    private final ConfiguredBlockSet deep;
    private final ConfiguredBlockSet rocky;

    /**
     * Creates all built-in marine formations with optional family-local overrides.
     *
     * @param options materializer configuration
     */
    MarineMaterialPalette(Map<String, String> options) {
        shallowWarm = ConfiguredBlockSet.parse(options, "blockset.ocean.shallow_warm",
                list(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.SAND,
                        Blocks.GRAVEL, Blocks.GRAVEL, Blocks.CLAY));
        shallowCold = ConfiguredBlockSet.parse(options, "blockset.ocean.shallow_cold",
                list(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.STONE,
                        Blocks.SAND, Blocks.CLAY));
        shelf = ConfiguredBlockSet.parse(options, "blockset.ocean.shelf",
                list(Blocks.SAND, Blocks.SAND, Blocks.GRAVEL, Blocks.GRAVEL,
                        Blocks.GRAVEL, Blocks.CLAY, Blocks.CLAY));
        deep = ConfiguredBlockSet.parse(options, "blockset.ocean.deep",
                list(Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, Blocks.GRAVEL,
                        Blocks.GRAVEL, Blocks.MUD, Blocks.STONE));
        rocky = ConfiguredBlockSet.parse(options, "blockset.ocean.rocky",
                list(Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.ANDESITE,
                        Blocks.ANDESITE, Blocks.GRAVEL));
    }

    /**
     * Returns the marine-bed state selected by physical depth and exposure bands.
     *
     * @param sample semantic terrain sample
     * @param x world X coordinate
     * @param y marine-bed Y coordinate
     * @param z world Z coordinate
     * @param seaLevel configured sea level
     * @return selected marine-bed state
     */
    BlockState bed(TerrainSample sample, int x, int y, int z, int seaLevel) {
        double depth = seaLevel - sample.surfaceHeight();
        if (sample.slope() > 0.85D) {
            return rocky.choose(sample, x, y, z);
        }
        if (depth <= 6.0D) {
            boolean cold = sample.climate().isAvailable()
                    && sample.climate().temperature() < 0.30D;
            return (cold ? shallowCold : shallowWarm).choose(sample, x, y, z);
        }
        if (depth <= 20.0D) {
            return shelf.choose(sample, x, y, z);
        }
        return deep.choose(sample, x, y, z);
    }

    private static List<BlockState> list(Block... blocks) {
        return Arrays.stream(blocks).map(Block::getDefaultState).toList();
    }
}
