package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
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
 * preserving caves and solid 3D substrate. Vanilla aquifer fluids are not
 * translated with the column because moving their absolute fluid levels can
 * expose deep lava or create unstable fluid cascades near the surface.</p>
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
                    BlockState current = source[y - minY];
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

        // The density/cave geometry may move vertically, but aquifer fluid levels
        // are absolute world-height phenomena. Translating water or lava together
        // with the column can move deep lava to spawn height and opens large fluid
        // fronts that immediately schedule thousands of neighbor updates. Treat a
        // translated fluid cell as an empty cave here. Surface/ocean water is
        // reconstructed separately above the engine surface.
        if (!state.getFluidState().isEmpty()) {
            return Blocks.AIR.getDefaultState();
        }
        return state;
    }

    private int waterTopExclusive(TerrainSample sample, int surfaceTop) {
        // Until the engine exposes a hydrologically consistent river-water level,
        // only fill columns up to the global sea level. The previous per-column
        // highland-river approximation created stepped source-water surfaces and
        // large fluid-update cascades.
        return clamp(Math.max(surfaceTop, seaLevel + 1), minY, maxYExclusive);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
