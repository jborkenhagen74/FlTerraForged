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

/** Repairs and protects the Engine-owned hydrologic envelope after vanilla cave carving. */
final class HydrologyCarverGuard {

    private final BlockMaterializer materializer;
    private final MaterializerContext context;
    private final int caveMargin;
    private final int sampleBorder;
    private final int sampleSize;

    /**
     * Creates a hydrology guard for the active replaceable materializer.
     *
     * @param materializer configured materializer
     */
    HydrologyCarverGuard(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
        this.caveMargin = Math.max(0, materializer.hydrologyCaveMargin());
        this.sampleBorder = Math.max(1, caveMargin);
        this.sampleSize = 16 + sampleBorder * 2;
    }

    /**
     * Restores river/lake beds, water columns and a configurable underground bank shell.
     *
     * <p>The vanilla carver still owns caves outside the hydrology margin. Concrete seal, bed and
     * fluid states as well as the protection dimensions are delegated to the configured
     * materializer.</p>
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
                WaterColumn column = columns[localZ + sampleBorder][localX + sampleBorder];
                if (column.wet()) {
                    restoreWetColumn(chunk, mutable, x, z, column);
                } else if (caveMargin > 0) {
                    sealProtectedBank(
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
        WaterColumn[][] columns = new WaterColumn[sampleSize][sampleSize];
        for (int sampleZ = 0; sampleZ < sampleSize; sampleZ++) {
            int z = pos.getStartZ() + sampleZ - sampleBorder;
            for (int sampleX = 0; sampleX < sampleSize; sampleX++) {
                int x = pos.getStartX() + sampleX - sampleBorder;
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
                column.surfaceY() - materializer.hydrologyBedSealDepth() + 1);
        for (int y = bedBottom; y < column.surfaceY(); y++) {
            set(chunk, mutable, x, y, z, materializer.hydrologySealState(sample, x, y, z));
        }
        set(
                chunk,
                mutable,
                x,
                column.surfaceY(),
                z,
                materializer.hydrologyBedState(sample, x, column.surfaceY(), z));
        for (int y = column.surfaceY() + 1; y < column.waterTopExclusive(); y++) {
            set(chunk, mutable, x, y, z, materializer.fluidState(sample));
        }
    }

    private void sealProtectedBank(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            WaterColumn column,
            WaterColumn[][] columns,
            int localX,
            int localZ) {
        int gridX = localX + sampleBorder;
        int gridZ = localZ + sampleBorder;
        int protectedWaterTop = Integer.MIN_VALUE;
        int radiusSq = caveMargin * caveMargin;
        for (int dz = -caveMargin; dz <= caveMargin; dz++) {
            for (int dx = -caveMargin; dx <= caveMargin; dx++) {
                if (dx == 0 && dz == 0 || dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                WaterColumn neighbor = columns[gridZ + dz][gridX + dx];
                if (neighbor.wet()) {
                    protectedWaterTop = Math.max(protectedWaterTop, neighbor.waterTopExclusive());
                }
            }
        }
        if (protectedWaterTop == Integer.MIN_VALUE) {
            return;
        }

        int top = Math.min(column.surfaceY() - 1, protectedWaterTop);
        int bottom = Math.max(
                context.minY() + 1,
                protectedWaterTop - materializer.hydrologyBankSealDepth());
        if (top < bottom) {
            return;
        }
        for (int y = bottom; y <= top; y++) {
            BlockState seal = materializer.hydrologySealState(column.sample(), x, y, z);
            set(chunk, mutable, x, y, z, seal);
        }
    }

    private static void set(
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
    }
}
