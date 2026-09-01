package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Final idempotent pass that restores Engine water columns and closes isolated surface gaps.
 *
 * <p>The pass never performs a free flood fill across arbitrary terrain. Exact Engine-owned wet
 * columns are always restored. A dry column is additionally repaired only when the active
 * materializer permits it and opposing wet neighbors prove that the column is part of a narrow,
 * enclosed hole in one continuous river/lake surface.</p>
 */
final class HydrologyFillPass {

    private final BlockMaterializer materializer;
    private final int repairRadius;
    private final int sampleBorder;
    private final int sampleSize;

    /**
     * Creates the fill pass for the active materializer.
     *
     * @param materializer configured replaceable materializer
     */
    HydrologyFillPass(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.repairRadius = Math.max(0, materializer.hydrologyGapRepairRadius());
        this.sampleBorder = Math.max(1, repairRadius);
        this.sampleSize = 16 + sampleBorder * 2;
    }

    /**
     * Restores materialized water and repairs isolated one-column gaps in continuous hydrology.
     *
     * @param chunk generated chunk
     * @param world bound Engine world
     */
    void apply(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        WaterColumn[][] columns = sampleEnvelope(pos, world);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; localZ++) {
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int x = pos.getStartX() + localX;
                int gridX = localX + sampleBorder;
                int gridZ = localZ + sampleBorder;
                WaterColumn column = columns[gridZ][gridX];

                if (column.hydrologyWet() || column.marineWet()) {
                    restoreExact(chunk, mutable, x, z, column);
                    continue;
                }

                int repairedTop = repairWaterTop(columns, gridX, gridZ, column.sample());
                if (repairedTop != Integer.MIN_VALUE) {
                    restoreGap(chunk, mutable, x, z, column.sample(), repairedTop);
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
                int bedY = materializer.solidSurfaceY(sample);
                int waterTop = materializer.waterTopExclusive(sample);
                boolean hydrologyWet = materializer.hasMaterializedWater(sample);
                boolean marineWet = (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                                || StandardTerrainTypes.COAST.equals(sample.terrainType()))
                        && waterTop > bedY + 1;
                columns[sampleZ][sampleX] = new WaterColumn(
                        sample,
                        bedY,
                        waterTop,
                        hydrologyWet,
                        marineWet);
            }
        }
        return columns;
    }

    private void restoreExact(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            WaterColumn column) {
        TerrainSample sample = column.sample();
        if (column.hydrologyWet()) {
            set(
                    chunk,
                    mutable,
                    x,
                    column.bedY(),
                    z,
                    materializer.hydrologyBedState(sample, x, column.bedY(), z));
        }
        BlockState fluid = materializer.fluidState(sample);
        for (int y = column.bedY() + 1; y < column.waterTopExclusive(); y++) {
            set(chunk, mutable, x, y, z, fluid);
        }
    }

    private int repairWaterTop(
            WaterColumn[][] columns,
            int gridX,
            int gridZ,
            TerrainSample sample) {
        if (repairRadius == 0 || !materializer.mayRepairHydrologyGap(sample)) {
            return Integer.MIN_VALUE;
        }

        int wetCount = 0;
        int minimumTop = Integer.MAX_VALUE;
        int maximumTop = Integer.MIN_VALUE;
        boolean north = false;
        boolean south = false;
        boolean west = false;
        boolean east = false;
        int radiusSquared = repairRadius * repairRadius;
        for (int offsetZ = -repairRadius; offsetZ <= repairRadius; offsetZ++) {
            for (int offsetX = -repairRadius; offsetX <= repairRadius; offsetX++) {
                if ((offsetX == 0 && offsetZ == 0)
                        || offsetX * offsetX + offsetZ * offsetZ > radiusSquared) {
                    continue;
                }
                WaterColumn neighbor = columns[gridZ + offsetZ][gridX + offsetX];
                if (!neighbor.hydrologyWet()) {
                    continue;
                }
                wetCount++;
                minimumTop = Math.min(minimumTop, neighbor.waterTopExclusive());
                maximumTop = Math.max(maximumTop, neighbor.waterTopExclusive());
                north |= offsetX == 0 && offsetZ < 0;
                south |= offsetX == 0 && offsetZ > 0;
                west |= offsetZ == 0 && offsetX < 0;
                east |= offsetZ == 0 && offsetX > 0;
            }
        }
        boolean oppositePair = north && south || west && east;
        int requiredWetEvidence = repairRadius == 1 ? 2 : 4;
        if (!oppositePair || wetCount < requiredWetEvidence) {
            return Integer.MIN_VALUE;
        }
        if (maximumTop - minimumTop > 1) {
            return Integer.MIN_VALUE;
        }
        // Prefer the lower neighboring level so a connectivity repair cannot create an isolated
        // one-block water pillar on a descending river.
        return minimumTop;
    }

    private void restoreGap(
            Chunk chunk,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample,
            int waterTopExclusive) {
        int bedY = materializer.hydrologyGapBedY(sample, waterTopExclusive);
        if (waterTopExclusive <= bedY + 1) {
            return;
        }
        set(chunk, mutable, x, bedY, z, materializer.hydrologyBedState(sample, x, bedY, z));
        BlockState fluid = materializer.fluidState(sample);
        for (int y = bedY + 1; y < waterTopExclusive; y++) {
            set(chunk, mutable, x, y, z, fluid);
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
            int bedY,
            int waterTopExclusive,
            boolean hydrologyWet,
            boolean marineWet) {
    }
}
