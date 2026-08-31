package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Final surface compatibility pass after vanilla surface rules.
 *
 * <p>Vanilla surface rules are retained for biome-owned variation. The guard
 * only enforces FlTerraForged semantics that vanilla cannot know about and
 * provides a deterministic grass/dirt fallback if the vanilla surface scan did
 * not reach an engine-shaped surface whose height differs from vanilla noise.</p>
 */
public final class EngineSurfaceGuard {

    private static final int FALLBACK_FILLER_DEPTH = 3;

    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;
    private final BlockState defaultBlock;
    private final TerrainMaterializer materializer;

    /**
     * Creates the surface guard for the active generation shape.
     *
     * @param minY minimum world Y
     * @param maxYExclusive exclusive maximum world Y
     * @param seaLevel world sea level
     * @param defaultBlock default solid substrate
     * @param materializer active vertical-resolution materializer
     */
    public EngineSurfaceGuard(
            int minY,
            int maxYExclusive,
            int seaLevel,
            BlockState defaultBlock,
            TerrainMaterializer materializer) {
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
        this.seaLevel = seaLevel;
        this.defaultBlock = defaultBlock;
        this.materializer = materializer;
    }

    /**
     * Applies semantic corrections and a safe top/filler fallback.
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
                int surfaceY = materializer.solidSurfaceY(
                        sample, minY, maxYExclusive);
                mutable.set(x, surfaceY, z);
                BlockState current = chunk.getBlockState(mutable);
                if (current.isAir() || !current.getFluidState().isEmpty()) {
                    continue;
                }

                BlockState forced = forcedTop(sample);
                if (forced != null) {
                    chunk.setBlockState(mutable, forced, false);
                    applyFallbackFiller(chunk, mutable, x, z, surfaceY, fillerFor(sample));
                    continue;
                }

                if (current.equals(defaultBlock)) {
                    BlockState top = fallbackTop(sample);
                    chunk.setBlockState(mutable, top, false);
                    applyFallbackFiller(chunk, mutable, x, z, surfaceY, fillerFor(sample));
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
            BlockState filler) {
        for (int depth = 1; depth <= FALLBACK_FILLER_DEPTH; depth++) {
            int y = surfaceY - depth;
            if (y <= minY) {
                break;
            }
            mutable.set(x, y, z);
            BlockState state = chunk.getBlockState(mutable);
            if (state.equals(defaultBlock)) {
                chunk.setBlockState(mutable, filler, false);
            }
        }
    }

    private BlockState forcedTop(TerrainSample sample) {
        if (StandardTerrainTypes.COAST.equals(sample.terrainType())) {
            return Blocks.SAND.getDefaultState();
        }
        if ((StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType()))
                && materializer.hasMaterializedWater(sample, minY, maxYExclusive)) {
            return Blocks.GRAVEL.getDefaultState();
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())) {
            return dryShore(sample)
                    ? Blocks.SAND.getDefaultState()
                    : Blocks.GRASS_BLOCK.getDefaultState();
        }
        if (RiparianZone.isDryBank(sample)) {
            return Blocks.GRASS_BLOCK.getDefaultState();
        }
        if (sample.climate().isAvailable()
                && sample.climate().temperature() < 0.20
                && sample.surfaceHeight() > seaLevel + 4) {
            return Blocks.SNOW_BLOCK.getDefaultState();
        }
        return null;
    }

    private BlockState fallbackTop(TerrainSample sample) {
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.GRASS_BLOCK.getDefaultState();
    }

    private BlockState fillerFor(TerrainSample sample) {
        if (RiparianZone.isDryBank(sample)) {
            return Blocks.DIRT.getDefaultState();
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())) {
            return dryShore(sample)
                    ? Blocks.SAND.getDefaultState()
                    : Blocks.DIRT.getDefaultState();
        }
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType())) {
            return Blocks.SAND.getDefaultState();
        }
        return Blocks.DIRT.getDefaultState();
    }


    private static boolean dryShore(TerrainSample sample) {
        return sample.climate().isAvailable()
                && (sample.climate().temperature() > 0.72D
                        || sample.climate().moisture() < 0.30D);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
