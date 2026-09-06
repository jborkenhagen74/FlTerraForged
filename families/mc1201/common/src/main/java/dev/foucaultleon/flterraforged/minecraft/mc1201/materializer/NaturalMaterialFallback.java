package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.engine.api.chunk.ColumnSnapshot;
import dev.foucaultleon.flterraforged.engine.api.chunk.NaturalMaterial;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Default compatibility mapping from Engine natural materials to the established materializer SPI. */
public final class NaturalMaterialFallback {

    private static final double MIN_CANONICAL_WET_DEPTH = 0.051D;

    private NaturalMaterialFallback() {
    }

    /**
     * Resolves an Engine material through the established block-materializer methods.
     *
     * <p>The immutable {@link ColumnSnapshot} is authoritative for whether a river/lake column is
     * wet. Older materializers may still derive that decision from {@link RiverSample}; for those
     * providers this compatibility adapter projects the canonical snapshot decision back into a
     * matching river sample. It never changes the actual block-volume water span.</p>
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
        TerrainSample sample = canonicalMaterialSample(column);
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

    private static TerrainSample canonicalMaterialSample(ColumnSnapshot column) {
        TerrainSample sample = column.terrain();
        boolean inlandHydrology = StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType());
        if (!inlandHydrology) {
            return sample;
        }

        RiverSample river = sample.river();
        boolean currentWet = river.hasWaterSurfaceHeight() && river.depth() > MIN_CANONICAL_WET_DEPTH;
        if (column.hasSurfaceWater() == currentWet) {
            return sample;
        }

        RiverSample canonical;
        if (column.hasSurfaceWater()) {
            double waterSurface = column.waterTopExclusive() - 1.0D;
            double depth = Math.max(MIN_CANONICAL_WET_DEPTH, waterSurface - sample.surfaceHeight());
            canonical = new RiverSample(
                    river.distance(),
                    river.width(),
                    depth,
                    waterSurface,
                    river.flow());
        } else {
            canonical = new RiverSample(
                    river.distance(),
                    river.width(),
                    0.0D,
                    Double.NaN,
                    river.flow());
        }
        return new TerrainSample(
                sample.surfaceHeight(),
                sample.slope(),
                sample.erosion(),
                sample.continentalness(),
                sample.terrainType(),
                sample.climate(),
                canonical);
    }
}
