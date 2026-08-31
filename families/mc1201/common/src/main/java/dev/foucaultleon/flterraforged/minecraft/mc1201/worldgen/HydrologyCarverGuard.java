package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/** Repairs the engine-owned hydrologic envelope after vanilla cave carving. */
final class HydrologyCarverGuard {

    private static final int SAMPLE_BORDER = 1;
    private static final int SAMPLE_SIZE = 16 + SAMPLE_BORDER * 2;
    private static final int BED_SEAL_DEPTH = 5;
    private static final int BANK_SEAL_DEPTH = 4;

    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;
    private final BlockState defaultBlock;
    private final BlockState defaultFluid;

    /** Creates a hydrology guard for the active generation shape. */
    HydrologyCarverGuard(
            int minY,
            int maxYExclusive,
            int seaLevel,
            BlockState defaultBlock,
            BlockState defaultFluid) {
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
        this.seaLevel = seaLevel;
        this.defaultBlock = defaultBlock;
        this.defaultFluid = defaultFluid;
    }

    /**
     * Restores river/lake beds, water columns and a one-block underground bank shell.
     *
     * <p>The vanilla carver still owns caves everywhere else. The guard only repairs columns with
     * materialized Engine water plus the subsurface side wall immediately adjacent to them.</p>
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
                            chunk, mutable, x, z, column, columns, localX, localZ);
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
                int surfaceY = clamp(
                        (int) Math.floor(sample.surfaceHeight()),
                        minY + 1,
                        maxYExclusive - 2);
                boolean wet = HydrologyColumn.hasMaterializedRiverWater(sample);
                int waterTopExclusive = wet
                        ? HydrologyColumn.waterTopExclusive(
                                sample,
                                surfaceY + 1,
                                seaLevel,
                                minY,
                                maxYExclusive)
                        : surfaceY + 1;
                columns[sampleZ][sampleX] = new WaterColumn(
                        surfaceY, waterTopExclusive, wet);
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
        int bedBottom = Math.max(minY + 1, column.surfaceY() - BED_SEAL_DEPTH + 1);
        for (int y = bedBottom; y < column.surfaceY(); y++) {
            set(chunk, mutable, x, y, z, defaultBlock);
        }
        set(chunk, mutable, x, column.surfaceY(), z, Blocks.GRAVEL.getDefaultState());
        for (int y = column.surfaceY() + 1; y < column.waterTopExclusive(); y++) {
            set(chunk, mutable, x, y, z, defaultFluid);
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
                Math.max(columns[gridZ][gridX - 1].protectedWaterTop(),
                        columns[gridZ][gridX + 1].protectedWaterTop()),
                Math.max(columns[gridZ - 1][gridX].protectedWaterTop(),
                        columns[gridZ + 1][gridX].protectedWaterTop()));
        if (adjacentWaterTop <= minY) {
            return;
        }

        int top = Math.min(column.surfaceY() - 1, adjacentWaterTop - 1);
        int bottom = Math.max(minY + 1, adjacentWaterTop - BANK_SEAL_DEPTH);
        if (top < bottom) {
            return;
        }
        for (int y = bottom; y <= top; y++) {
            set(chunk, mutable, x, y, z, defaultBlock);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record WaterColumn(int surfaceY, int waterTopExclusive, boolean wet) {

        int protectedWaterTop() {
            return wet ? waterTopExclusive : Integer.MIN_VALUE;
        }
    }
}
