package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

/**
 * Adapts Minecraft's vanilla three-dimensional substrate to the surface height produced by the
 * external terrain engine without translating the substrate in the vertical axis.
 *
 * <p>All newly emitted blocks and fluids are selected by the configured {@link BlockMaterializer}.
 * The bridge therefore owns geometry reconciliation only; concrete block choices remain replaceable
 * by an add-on materializer.</p>
 */
public final class EngineDensityBridge {

    private static final int SURFACE_SEAL_DEPTH = 6;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates a bridge for the active materializer context.
     *
     * @param materializer active replaceable materializer
     */
    public EngineDensityBridge(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Reconciles all vanilla noise-filled columns with the Engine surface.
     *
     * @param chunk vanilla noise-filled chunk
     * @param world bound external terrain world
     */
    public void reshape(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockState[] source = new BlockState[context.maxYExclusive() - context.minY()];

        for (int localZ = 0; localZ < 16; localZ++) {
            int blockZ = pos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int blockX = pos.getStartX() + localX;
                TerrainSample sample = world.sample(blockX, blockZ);

                snapshotColumn(chunk, blockX, blockZ, mutable, source);
                int sourceSurfaceY = findSourceSurface(source);
                int targetSurfaceY = materializer.solidSurfaceY(sample);
                int sealBottomY = Math.max(
                        context.minY() + 1,
                        targetSurfaceY - SURFACE_SEAL_DEPTH + 1);
                int waterTopExclusive = materializer.waterTopExclusive(sample);

                for (int y = context.minY(); y < context.maxYExclusive(); y++) {
                    BlockState state = reconciledState(
                            sample,
                            source,
                            y,
                            sourceSurfaceY,
                            targetSurfaceY,
                            sealBottomY,
                            waterTopExclusive);
                    BlockState current = source[y - context.minY()];
                    if (!state.equals(current)) {
                        mutable.set(blockX, y, blockZ);
                        chunk.setBlockState(mutable, state, false);
                    }
                }
            }
        }

        Heightmap.populateHeightmaps(chunk, java.util.Set.of(
                Heightmap.Type.OCEAN_FLOOR_WG,
                Heightmap.Type.WORLD_SURFACE_WG));
    }

    private void snapshotColumn(
            Chunk chunk,
            int blockX,
            int blockZ,
            BlockPos.Mutable mutable,
            BlockState[] target) {
        for (int y = context.minY(); y < context.maxYExclusive(); y++) {
            mutable.set(blockX, y, blockZ);
            target[y - context.minY()] = chunk.getBlockState(mutable);
        }
    }

    private int findSourceSurface(BlockState[] source) {
        for (int y = context.maxYExclusive() - 1; y >= context.minY(); y--) {
            BlockState state = source[y - context.minY()];
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return context.minY();
    }

    private BlockState reconciledState(
            TerrainSample sample,
            BlockState[] source,
            int y,
            int sourceSurfaceY,
            int targetSurfaceY,
            int sealBottomY,
            int waterTopExclusive) {
        if (y == context.minY()) {
            BlockState floor = source[0];
            return floor == null || floor.isAir()
                    ? materializer.bedrockState(sample)
                    : floor;
        }

        if (y > targetSurfaceY) {
            return y < waterTopExclusive
                    ? materializer.fluidState(sample)
                    : materializer.airState(sample);
        }

        if (y >= sealBottomY) {
            return materializer.surfaceSealState(sample);
        }

        if (y > sourceSurfaceY) {
            return materializer.substrateState(sample);
        }

        BlockState state = source[y - context.minY()];
        return state == null ? materializer.substrateState(sample) : state;
    }
}
