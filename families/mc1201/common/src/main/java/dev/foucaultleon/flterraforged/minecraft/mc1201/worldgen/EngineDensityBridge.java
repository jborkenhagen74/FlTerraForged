package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

/**
 * Bridges Minecraft's three-dimensional vanilla density/aquifer substrate to
 * the two-dimensional surface height produced by the external terrain engine.
 *
 * <p>The vanilla noise generator first fills a chunk using the configured
 * {@link ChunkGeneratorSettings}. This bridge then vertically remaps each
 * column so that its solid density boundary follows the engine surface while
 * preserving caves, aquifer pockets, ore-vein substrate and other 3D states
 * already present in the vanilla density result.</p>
 */
public final class EngineDensityBridge {

    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;
    private final BlockState defaultBlock;
    private final BlockState defaultFluid;

    /** Creates a bridge for one Minecraft generation-shape configuration. */
    public EngineDensityBridge(ChunkGeneratorSettings settings) {
        this.minY = settings.generationShapeConfig().minimumY();
        this.maxYExclusive = minY + settings.generationShapeConfig().height();
        this.seaLevel = settings.seaLevel();
        this.defaultBlock = settings.defaultBlock();
        this.defaultFluid = settings.defaultFluid();
    }

    /**
     * Remaps all columns in {@code chunk} to the engine surface.
     *
     * @param chunk vanilla noise-filled chunk
     * @param world bound external terrain world
     */
    public void reshape(Chunk chunk, TerrainWorld world) {
        ChunkPos pos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockState[] source = new BlockState[maxYExclusive - minY];

        for (int localZ = 0; localZ < 16; localZ++) {
            int blockZ = pos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int blockX = pos.getStartX() + localX;
                TerrainSample sample = world.sample(blockX, blockZ);

                snapshotColumn(chunk, blockX, blockZ, mutable, source);
                int sourceSurfaceY = findSourceSurface(source);
                int targetSurfaceY = clamp(
                        (int) Math.floor(sample.surfaceHeight()),
                        minY + 1,
                        maxYExclusive - 2);
                int delta = targetSurfaceY - sourceSurfaceY;
                int waterTopExclusive = waterTopExclusive(sample, targetSurfaceY + 1);

                for (int y = minY; y < maxYExclusive; y++) {
                    BlockState state = remapState(source, y, targetSurfaceY, delta, waterTopExclusive);
                    mutable.set(blockX, y, blockZ);
                    chunk.setBlockState(mutable, state, false);
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
        for (int y = minY; y < maxYExclusive; y++) {
            mutable.set(blockX, y, blockZ);
            target[y - minY] = chunk.getBlockState(mutable);
        }
    }

    private int findSourceSurface(BlockState[] source) {
        for (int y = maxYExclusive - 1; y >= minY; y--) {
            BlockState state = source[y - minY];
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return seaLevel;
    }

    private BlockState remapState(
            BlockState[] source,
            int y,
            int targetSurfaceY,
            int delta,
            int waterTopExclusive) {
        if (y == minY) {
            return Blocks.BEDROCK.getDefaultState();
        }
        if (y > targetSurfaceY) {
            return y < waterTopExclusive ? defaultFluid : Blocks.AIR.getDefaultState();
        }

        int sourceY = y - delta;
        if (sourceY <= minY || sourceY >= maxYExclusive) {
            return defaultBlock;
        }
        BlockState state = source[sourceY - minY];
        if (state == null) {
            return defaultBlock;
        }

        // Do not let the translated vanilla sea overwrite land below the engine
        // surface. Genuine underground aquifer fluid is retained because it is
        // surrounded by non-air density and is below the target surface.
        if (!state.getFluidState().isEmpty() && y >= targetSurfaceY - 1) {
            return defaultBlock;
        }
        return state;
    }

    private int waterTopExclusive(TerrainSample sample, int surfaceTop) {
        int waterTop = seaLevel + 1;
        if (StandardTerrainTypes.RIVER.equals(sample.terrainType()) && sample.river().isAvailable()) {
            double riverWater = sample.surfaceHeight()
                    + Math.min(2.0, Math.max(0.5, sample.river().depth() * 0.25));
            waterTop = Math.max(waterTop, (int) Math.ceil(riverWater) + 1);
        }
        return clamp(Math.max(surfaceTop, waterTop), minY, maxYExclusive);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
