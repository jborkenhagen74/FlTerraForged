package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Final surface compatibility pass after vanilla surface rules.
 *
 * <p>The guard owns only the decision of <em>when</em> a surface needs correction. Every concrete
 * block emitted by the pass comes from the selected {@link BlockMaterializer}.</p>
 */
public final class EngineSurfaceGuard {

    private static final int FALLBACK_FILLER_DEPTH = 3;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates the surface guard for the active materializer.
     *
     * @param materializer configured replaceable materializer
     */
    public EngineSurfaceGuard(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Applies semantic corrections and a safe materializer-owned fallback.
     *
     * @param chunk chunk whose surface is being corrected
     * @param world bound external terrain world
     */
    public void apply(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int localZ = 0; localZ < 16; localZ++) {
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int x = pos.getStartX() + localX;
                TerrainSample sample = world.sample(x, z);
                int surfaceY = materializer.solidSurfaceY(sample);
                mutable.set(x, surfaceY, z);
                BlockState current = chunk.getBlockState(mutable);
                if (current.isAir() || !current.getFluidState().isEmpty()) {
                    continue;
                }

                Optional<BlockState> forced = materializer.forcedSurfaceState(sample);
                if (forced.isPresent()) {
                    chunk.setBlockState(mutable, forced.get(), false);
                    applyFallbackFiller(
                            chunk,
                            mutable,
                            x,
                            z,
                            surfaceY,
                            materializer.fillerState(sample),
                            materializer.substrateState(sample));
                    continue;
                }

                BlockState substrate = materializer.substrateState(sample);
                if (current.equals(substrate)) {
                    chunk.setBlockState(
                            mutable,
                            materializer.fallbackSurfaceState(sample),
                            false);
                    applyFallbackFiller(
                            chunk,
                            mutable,
                            x,
                            z,
                            surfaceY,
                            materializer.fillerState(sample),
                            substrate);
                }
            }
        }
    }

    private void applyFallbackFiller(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            int surfaceY,
            BlockState filler,
            BlockState substrate) {
        for (int depth = 1; depth <= FALLBACK_FILLER_DEPTH; depth++) {
            int y = surfaceY - depth;
            if (y <= context.minY()) {
                break;
            }
            mutable.set(x, y, z);
            BlockState state = chunk.getBlockState(mutable);
            if (state.equals(substrate)) {
                chunk.setBlockState(mutable, filler, false);
            }
        }
    }
}
