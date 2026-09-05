package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import net.minecraft.block.BlockState;

/** Composes the synchronous block-column representation through the active materializer. */
public final class ColumnComposer {

    private static final int FILLER_DEPTH = 3;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates a column composer using the configured replaceable materializer.
     *
     * @param materializer active block materializer
     */
    public ColumnComposer(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Returns the first air block above the legacy position-independent solid surface.
     *
     * @param sample continuous Engine terrain sample
     * @return first block above the solid surface
     */
    public int surfaceTop(TerrainSample sample) {
        return materializer.solidSurfaceTop(sample);
    }

    /**
     * Returns the first air block above the provider's physical surface at one world column.
     *
     * <p>Heightmaps operate on occupied Minecraft cells, so a partial-height top still contributes
     * {@code blockY + 1} even though its physical {@code topY} may be lower.</p>
     *
     * @param sample continuous Engine terrain sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return first block cell above the physical top material
     */
    public int surfaceTop(TerrainSample sample, int x, int z) {
        MaterializedSurfaceGeometry geometry =
                MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
        return geometry.blockY() + 1;
    }

    /**
     * Returns the first block above solid terrain plus ocean or Engine hydrology water.
     *
     * @param sample continuous Engine terrain sample
     * @return first block above the complete world surface
     */
    public int worldSurfaceTop(TerrainSample sample) {
        return materializer.waterTopExclusive(sample);
    }

    /**
     * Returns the first block above the complete position-aware world surface.
     *
     * @param sample continuous Engine terrain sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return first block above provider geometry and materialized water
     */
    public int worldSurfaceTop(TerrainSample sample, int x, int z) {
        return Math.max(
                surfaceTop(sample, x, z),
                materializer.waterTopExclusive(sample));
    }

    /**
     * Builds a full vertical column suitable for chunk fill and structure sampling.
     *
     * @param sample continuous Engine terrain sample
     * @return block states from minimum Y through the exclusive maximum Y
     */
    public BlockState[] compose(TerrainSample sample) {
        return compose(sample, 0, 0);
    }

    /**
     * Builds a position-aware vertical column suitable for chunk fill and structure sampling.
     *
     * <p>The provider's physical top-cell geometry is authoritative. Water that shares a partial top
     * cell is materialized only through the provider's waterlogging hooks; non-waterloggable partial
     * states are retained and full fluid starts in the first complete cell above them.</p>
     *
     * @param sample continuous Engine terrain sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return block states from minimum Y through the exclusive maximum Y
     */
    public BlockState[] compose(TerrainSample sample, int x, int z) {
        BlockState[] states = new BlockState[context.maxYExclusive() - context.minY()];
        MaterializedSurfaceGeometry geometry =
                MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
        int surfaceY = geometry.blockY();
        int waterTopExclusive = materializer.waterTopExclusive(sample);
        int firstWaterY = MaterializerGeometry.firstWaterY(
                materializer,
                sample,
                geometry,
                x,
                z,
                waterTopExclusive);
        BlockState top = materializer.composedTopState(sample, x, z);
        if (firstWaterY == surfaceY
                && surfaceY < waterTopExclusive
                && materializer.permitsFinalWetFlow(sample, top, x, surfaceY, z)) {
            top = materializer.finalWetState(sample, top, x, surfaceY, z);
        }
        BlockState filler = materializer.fillerState(sample, x, surfaceY - 1, z);
        BlockState substrate = materializer.substrateState(sample);
        BlockState fluid = materializer.fluidState(sample);
        BlockState air = materializer.airState(sample);

        for (int y = context.minY(); y < context.maxYExclusive(); y++) {
            BlockState state;
            if (y == context.minY()) {
                state = materializer.bedrockState(sample);
            } else if (y < surfaceY - FILLER_DEPTH + 1) {
                state = substrate;
            } else if (y < surfaceY) {
                state = filler;
            } else if (y == surfaceY) {
                state = top;
            } else if (y >= firstWaterY && y < waterTopExclusive) {
                state = fluid;
            } else {
                state = air;
            }
            states[y - context.minY()] = state;
        }
        return states;
    }
}
