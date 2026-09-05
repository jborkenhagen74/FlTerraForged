package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
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
 *
 * <p>R54 resolves wetness against the physical surface geometry reported by the selected block
 * provider instead of assuming that every top block occupies a complete one-block cell. This keeps
 * variable-height materializers compatible with the same Engine hydrology and allows waterlogging
 * providers to realize the submerged fraction of a partial surface cell without moving block
 * selection into the Engine.</p>
 */
final class HydrologyFillPass {

    private static final double PHYSICAL_EPSILON = 1.0E-6D;
    private static final int VERTICAL_DROP_MINIMUM = 2;
    private static final double WATERFALL_CORE_FRACTION = 0.50D;

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
                    int bedY = column.hydrologyWet()
                            ? resolvedBedY(columns, gridX, gridZ, column)
                            : column.bedY();
                    int restoreTop = column.hydrologyWet()
                            ? verticalDropTopExclusive(columns, gridX, gridZ, column)
                            : column.waterTopExclusive();
                    restoreExact(chunk, mutable, x, z, column, bedY, restoreTop);
                    continue;
                }

                int repairedTop = repairWaterTop(columns, gridX, gridZ, column.sample());
                if (repairedTop != Integer.MIN_VALUE) {
                    restoreGap(chunk, mutable, x, z, column, repairedTop);
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
                MaterializedSurfaceGeometry geometry =
                        MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
                int bedY = geometry.blockY();
                int waterTop = materializer.waterTopExclusive(sample);
                RiverSample hydrology = sample.river();
                boolean providerWetHint = materializer.hasMaterializedWater(sample);
                boolean physicallyWetHydrology = hydrology.hasWaterSurfaceHeight()
                        && hydrology.depth() > PHYSICAL_EPSILON
                        && hydrology.waterSurfaceHeight() > geometry.topY() + PHYSICAL_EPSILON
                        && waterTop > geometry.topY() + PHYSICAL_EPSILON;
                // Preserve the legacy materializer decision for conventional full-block providers.
                // Partial-height providers may legitimately report false through the old full-cell
                // test while their continuous topY still leaves physical room for Engine water.
                boolean hydrologyWet = physicallyWetHydrology
                        && (providerWetHint
                                || geometry.occupiedHeight() < 1.0D - PHYSICAL_EPSILON);
                boolean marineWet = (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                                || StandardTerrainTypes.COAST.equals(sample.terrainType()))
                        && waterTop > geometry.topY() + PHYSICAL_EPSILON;
                columns[sampleZ][sampleX] = new WaterColumn(
                        sample,
                        geometry,
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
            WaterColumn column,
            int bedY,
            int waterTopExclusive) {
        TerrainSample sample = column.sample();
        if (column.hydrologyWet()) {
            BlockState dryBed = materializer.hydrologyBedState(sample, x, bedY, z);
            set(
                    chunk,
                    mutable,
                    x,
                    bedY,
                    z,
                    submergedSurfaceState(column, x, z, bedY, waterTopExclusive, dryBed));
        }
        BlockState fluid = materializer.fluidState(sample);
        for (int y = bedY + 1; y < waterTopExclusive; y++) {
            set(chunk, mutable, x, y, z, fluid);
        }
    }

    private int resolvedBedY(
            WaterColumn[][] columns,
            int gridX,
            int gridZ,
            WaterColumn center) {
        // Provider-reported partial geometry is authoritative. Moving a half-height or layered
        // surface to a neighboring integer Y would destroy the provider's physical model. Full
        // blocks retain the original one-block bed smoothing.
        if (center.geometry().occupiedHeight() < 1.0D - PHYSICAL_EPSILON) {
            return center.bedY();
        }
        return smoothedHydrologyBedY(columns, gridX, gridZ, center);
    }

    private static int smoothedHydrologyBedY(
            WaterColumn[][] columns,
            int gridX,
            int gridZ,
            WaterColumn center) {
        WaterColumn north = columns[gridZ - 1][gridX];
        WaterColumn south = columns[gridZ + 1][gridX];
        WaterColumn west = columns[gridZ][gridX - 1];
        WaterColumn east = columns[gridZ][gridX + 1];
        int target = center.bedY();
        if (sameSurface(north, center) && sameSurface(south, center)) {
            target = Math.min(target, Math.max(north.bedY(), south.bedY()) + 1);
        }
        if (sameSurface(west, center) && sameSurface(east, center)) {
            target = Math.min(target, Math.max(west.bedY(), east.bedY()) + 1);
        }
        return target;
    }

    private static boolean sameSurface(WaterColumn candidate, WaterColumn center) {
        return candidate.hydrologyWet()
                && Math.abs(candidate.waterTopExclusive() - center.waterTopExclusive()) <= 1;
    }

    /**
     * Extends a lower river-core column upward when an immediately adjacent core column carries a
     * substantially higher resolved water surface.
     *
     * <p>The Engine R44 path remains the authority for the two horizontal surface levels. This host
     * step only realizes the vertical face between them, turning a two-plus-block discontinuity into
     * a continuous cascade/waterfall column instead of leaving an air gap between independently
     * materialized river surfaces.</p>
     */
    private static int verticalDropTopExclusive(
            WaterColumn[][] columns,
            int gridX,
            int gridZ,
            WaterColumn center) {
        int ownTop = center.waterTopExclusive();
        if (!isRiverCore(center)) {
            return ownTop;
        }

        int highest = ownTop;
        WaterColumn north = columns[gridZ - 1][gridX];
        WaterColumn south = columns[gridZ + 1][gridX];
        WaterColumn west = columns[gridZ][gridX - 1];
        WaterColumn east = columns[gridZ][gridX + 1];
        highest = higherAdjacentCoreTop(highest, ownTop, north);
        highest = higherAdjacentCoreTop(highest, ownTop, south);
        highest = higherAdjacentCoreTop(highest, ownTop, west);
        highest = higherAdjacentCoreTop(highest, ownTop, east);
        return highest;
    }

    private static int higherAdjacentCoreTop(int current, int ownTop, WaterColumn candidate) {
        if (!candidate.hydrologyWet() || !isRiverCore(candidate)) {
            return current;
        }
        int candidateTop = candidate.waterTopExclusive();
        return candidateTop - ownTop >= VERTICAL_DROP_MINIMUM
                ? Math.max(current, candidateTop)
                : current;
    }

    private static boolean isRiverCore(WaterColumn column) {
        if (!StandardTerrainTypes.RIVER.equals(column.sample().terrainType())) {
            return false;
        }
        RiverSample river = column.sample().river();
        if (!river.hasWaterSurfaceHeight() || river.width() <= 0.0D) {
            return false;
        }
        double halfWidth = Math.max(0.5D, river.width() * 0.5D);
        return Math.abs(river.distance()) <= Math.max(1.0D, halfWidth * WATERFALL_CORE_FRACTION);
    }

    private BlockState submergedSurfaceState(
            WaterColumn column,
            int x,
            int z,
            int bedY,
            int waterTopExclusive,
            BlockState dryState) {
        MaterializedSurfaceGeometry geometry = column.geometry();
        if (geometry.blockY() != bedY
                || geometry.occupiedHeight() >= 1.0D - PHYSICAL_EPSILON
                || !materializer.capabilities().waterlogging()
                || waterTopExclusive <= geometry.topY() + PHYSICAL_EPSILON) {
            return dryState;
        }
        return Objects.requireNonNull(
                materializer.submergedHydrologySurfaceState(
                        column.sample(), x, bedY, z, dryState),
                "BlockMaterializer returned null submerged hydrology state");
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
            WaterColumn column,
            int waterTopExclusive) {
        TerrainSample sample = column.sample();
        int bedY = materializer.hydrologyGapBedY(sample, waterTopExclusive);
        if (waterTopExclusive <= bedY + 1) {
            return;
        }
        BlockState dryBed = materializer.hydrologyBedState(sample, x, bedY, z);
        set(
                chunk,
                mutable,
                x,
                bedY,
                z,
                submergedSurfaceState(column, x, z, bedY, waterTopExclusive, dryBed));
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
            MaterializedSurfaceGeometry geometry,
            int bedY,
            int waterTopExclusive,
            boolean hydrologyWet,
            boolean marineWet) {
    }
}
