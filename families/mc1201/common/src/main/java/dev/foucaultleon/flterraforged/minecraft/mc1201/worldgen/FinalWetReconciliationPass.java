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

/**
 * Performs the single post-carver reconciliation of connected Engine-owned water.
 *
 * <p>The pass deliberately never reconstructs beds, banks or other solid geometry. Vanilla carvers
 * may therefore cut the sea floor and river/lake beds naturally. After all carving has completed,
 * water is propagated from each Engine-defined wet surface through air or provider-approved
 * water-bearing cells, but only inside columns that belong to the Engine/materializer wet envelope.
 * Disconnected caves remain dry.</p>
 */
final class FinalWetReconciliationPass {

    private static final int CHUNK_SIZE = 16;
    private static final int COLUMN_COUNT = CHUNK_SIZE * CHUNK_SIZE;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates the final wet reconciliation pass.
     *
     * @param materializer active replaceable materializer
     */
    FinalWetReconciliationPass(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Restores missing connected water exactly once after all carvers and before native features.
     *
     * @param chunk chunk whose destructive terrain passes have completed
     * @param world bound Engine terrain world
     */
    void apply(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        TerrainSample[] samples = world.sampleTile(pos.getStartX(), pos.getStartZ(), CHUNK_SIZE);
        if (samples.length != COLUMN_COUNT) {
            throw new IllegalStateException("TerrainWorld returned an invalid 16x16 sample tile");
        }

        boolean[] wet = new boolean[COLUMN_COUNT];
        int[] waterTopExclusive = new int[COLUMN_COUNT];
        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int x = pos.getStartX() + localX;
                int column = columnIndex(localX, localZ);
                TerrainSample sample = samples[column];
                int waterTop = clamp(
                        materializer.waterTopExclusive(sample),
                        context.minY(),
                        context.maxYExclusive());
                wet[column] = materializer.hasFinalWetEnvelope(sample, x, z)
                        && waterTop > context.minY();
                waterTopExclusive[column] = waterTop;
            }
        }

        int worldHeight = context.maxYExclusive() - context.minY();
        boolean[] visited = new boolean[COLUMN_COUNT * worldHeight];
        int[] queue = new int[visited.length];
        int head = 0;
        int tail = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // Every Engine-defined wet column contributes one virtual water-surface seed. Missing top
        // water therefore does not prevent reconciliation, while an intact solid floor naturally
        // stops the flood from entering a disconnected cave below it.
        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int column = columnIndex(localX, localZ);
                if (!wet[column]) {
                    continue;
                }
                int y = waterTopExclusive[column] - 1;
                if (y < context.minY() || y >= context.maxYExclusive()) {
                    continue;
                }
                int x = pos.getStartX() + localX;
                if (canFlowThrough(chunk, mutable, samples[column], x, y, z)) {
                    tail = enqueue(queue, visited, tail, column, y);
                }
            }
        }

        while (head < tail) {
            int packed = queue[head++];
            int vertical = packed / COLUMN_COUNT;
            int column = packed - vertical * COLUMN_COUNT;
            int localX = column & 15;
            int localZ = column >>> 4;
            int x = pos.getStartX() + localX;
            int z = pos.getStartZ() + localZ;
            int y = context.minY() + vertical;
            TerrainSample sample = samples[column];

            mutable.set(x, y, z);
            BlockState current = chunk.getBlockState(mutable);
            if (!materializer.permitsFinalWetFlow(sample, current, x, y, z)) {
                continue;
            }
            BlockState target = materializer.finalWetState(sample, current, x, y, z);
            if (target.getFluidState().isEmpty()) {
                throw new IllegalStateException(
                        "BlockMaterializer finalWetState must contain fluid at " + x + "," + y + "," + z);
            }
            if (!target.equals(current)) {
                chunk.setBlockState(mutable, target, false);
            }

            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX,
                    localZ,
                    y - 1,
                    pos);
            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX,
                    localZ,
                    y + 1,
                    pos);
            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX - 1,
                    localZ,
                    y,
                    pos);
            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX + 1,
                    localZ,
                    y,
                    pos);
            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX,
                    localZ - 1,
                    y,
                    pos);
            tail = enqueueNeighbor(
                    chunk,
                    mutable,
                    samples,
                    wet,
                    waterTopExclusive,
                    queue,
                    visited,
                    tail,
                    localX,
                    localZ + 1,
                    y,
                    pos);
        }
    }

    private int enqueueNeighbor(
            Chunk chunk,
            BlockPos.Mutable mutable,
            TerrainSample[] samples,
            boolean[] wet,
            int[] waterTopExclusive,
            int[] queue,
            boolean[] visited,
            int tail,
            int localX,
            int localZ,
            int y,
            ChunkPos pos) {
        if (localX < 0
                || localX >= CHUNK_SIZE
                || localZ < 0
                || localZ >= CHUNK_SIZE
                || y < context.minY()
                || y >= context.maxYExclusive()) {
            return tail;
        }
        int column = columnIndex(localX, localZ);
        if (!wet[column] || y >= waterTopExclusive[column]) {
            return tail;
        }
        int packed = pack(column, y);
        if (visited[packed]) {
            return tail;
        }
        int x = pos.getStartX() + localX;
        int z = pos.getStartZ() + localZ;
        if (!canFlowThrough(chunk, mutable, samples[column], x, y, z)) {
            visited[packed] = true;
            return tail;
        }
        return enqueue(queue, visited, tail, column, y);
    }

    private boolean canFlowThrough(
            Chunk chunk,
            BlockPos.Mutable mutable,
            TerrainSample sample,
            int x,
            int y,
            int z) {
        mutable.set(x, y, z);
        return materializer.permitsFinalWetFlow(sample, chunk.getBlockState(mutable), x, y, z);
    }

    private int enqueue(int[] queue, boolean[] visited, int tail, int column, int y) {
        int packed = pack(column, y);
        if (visited[packed]) {
            return tail;
        }
        visited[packed] = true;
        queue[tail] = packed;
        return tail + 1;
    }

    private int pack(int column, int y) {
        return (y - context.minY()) * COLUMN_COUNT + column;
    }

    private static int columnIndex(int localX, int localZ) {
        return localZ * CHUNK_SIZE + localX;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
