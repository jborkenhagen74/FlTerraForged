package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/** Repairs the Engine-owned hydrologic envelope after vanilla cave carving. */
final class HydrologyCarverGuard {

    private static final int SAMPLE_BORDER = 1;
    private static final int SAMPLE_SIZE = 16 + SAMPLE_BORDER * 2;
    private static final int BED_SEAL_DEPTH = 5;
    private static final int BANK_SEAL_DEPTH = 4;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates a hydrology guard for the active replaceable materializer.
     *
     * @param materializer configured materializer
     */
    HydrologyCarverGuard(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Restores river/lake beds, water columns and the current underground bank shell.
     *
     * <p>The vanilla carver still owns caves everywhere else. Concrete seal, bed and fluid states
     * are delegated to the configured materializer.</p>
     *
     * @param chunk carved chunk
     * @param world bound Engine world
     */
    void repair(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        WaterColumn[][] columns = sampleEnvelope(pos, world);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; localZ++) {
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int x = pos.getStartX() + localX;
                WaterColumn column = columns[localZ + SAMPLE_BORDER][localX + SAMPLE_BORDER];
                if (column.wet()) {
                    restoreWetColumn(chunk, mutable, x, z, column);
                } else {
                    sealAdjacentBank(
                            chunk,
                            mutable,
                            x,
                            z,
                            column,
                            columns,
                            localX,
                            localZ);
                }
            }
        }
    }

    private WaterColumn[][] sampleEnvelope(ChunkPos pos, TerrainWorld world) {
        WaterColumn[][] columns = new WaterColumn[SAMPLE_SIZE][SAMPLE_SIZE];
        for (int sampleZ = 0; sampleZ < SAMPLE_SIZE; sampleZ++) {
            int z = pos.getStartZ() + sampleZ - SAMPLE_BORDER;
            for (int sampleX = 0; sampleX < SAMPLE_SIZE; sampleX++) {
                int x = pos.getStartX() + sampleX - SAMPLE_BORDER;
                TerrainSample sample = world.sample(x, z);
                int surfaceY = materializer.solidSurfaceY(sample);
                boolean wet = materializer.hasMaterializedWater(sample);
                int waterTopExclusive = wet
                        ? materializer.waterTopExclusive(sample)
                        : surfaceY + 1;
                columns[sampleZ][sampleX] = new WaterColumn(
                        sample,
                        surfaceY,
                        waterTopExclusive,
                        wet);
            }
        }
        return columns;
    }

    private void restoreWetColumn(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            WaterColumn column) {
        TerrainSample sample = column.sample();
        int bedBottom = Math.max(
                context.minY() + 1,
                column.surfaceY() - BED_SEAL_DEPTH + 1);
        for (int y = bedBottom; y < column.surfaceY(); y++) {
            set(chunk, mutable, x, y, z, materializer.hydrologySealState(sample));
        }
        set(
                chunk,
                mutable,
                x,
                column.surfaceY(),
                z,
                materializer.hydrologyBedState(sample));
        for (int y = column.surfaceY() + 1; y < column.waterTopExclusive(); y++) {
            set(chunk, mutable, x, y, z, materializer.fluidState(sample));
        }
    }

    private void sealAdjacentBank(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            WaterColumn column,
            WaterColumn[][] columns,
            int localX,
            int localZ) {
        int gridX = localX + SAMPLE_BORDER;
        int gridZ = localZ + SAMPLE_BORDER;
        int adjacentWaterTop = Math.max(
                Math.max(
                        columns[gridZ][gridX - 1].protectedWaterTop(),
                        columns[gridZ][gridX + 1].protectedWaterTop()),
                Math.max(
                        columns[gridZ - 1][gridX].protectedWaterTop(),
                        columns[gridZ + 1][gridX].protectedWaterTop()));
        if (adjacentWaterTop <= context.minY()) {
            return;
        }

        int top = Math.min(column.surfaceY() - 1, adjacentWaterTop - 1);
        int bottom = Math.max(context.minY() + 1, adjacentWaterTop - BANK_SEAL_DEPTH);
        if (top < bottom) {
            return;
        }
        BlockState seal = materializer.hydrologySealState(column.sample());
        for (int y = bottom; y <= top; y++) {
            set(chunk, mutable, x, y, z, seal);
        }
    }

    private void set(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state) {
        mutable.set(x, y, z);
        if (!chunk.getBlockState(mutable).equals(state)) {
            chunk.setBlockState(mutable, state, false);
        }
    }

    private record WaterColumn(
            TerrainSample sample,
            int surfaceY,
            int waterTopExclusive,
            boolean wet) {

        int protectedWaterTop() {
            return wet ? waterTopExclusive : Integer.MIN_VALUE;
        }
    }
}
