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
 * Adapts Minecraft's vanilla three-dimensional substrate to the surface height
 * produced by the external terrain engine without translating the substrate in
 * the vertical axis.
 *
 * <p>Earlier revisions moved every vanilla column up or down by the difference
 * between the vanilla and engine surfaces. Adjacent columns normally have
 * different deltas, so caves, aquifers and stone layers were sheared apart and
 * appeared as floating plates, horizontal gaps and vertical walls. This bridge
 * keeps every vanilla substrate state at its original absolute Y coordinate.
 * It only truncates material above the engine surface or adds solid material
 * when the engine surface is higher.</p>
 *
 * <p>A small solid cap is enforced below the engine surface before vanilla
 * carvers run. This prevents a pre-existing vanilla cave from becoming an
 * accidental paper-thin roof solely because the engine surface intersects it.
 * Normal cave mouths may still be created later by Minecraft's carver stage.</p>
 */
public final class EngineDensityBridge {

    private static final int SURFACE_SEAL_DEPTH = 6;

    private final int minY;
    private final int maxYExclusive;
    private final int seaLevel;
    private final BlockState defaultBlock;
    private final BlockState defaultFluid;
    private final TerrainMaterializer materializer;

    /**
     * Creates a bridge for one Minecraft generation-shape configuration.
     *
     * @param settings active vanilla chunk-generator settings
     * @param materializer active vertical-resolution materializer
     */
    public EngineDensityBridge(
            ChunkGeneratorSettings settings,
            TerrainMaterializer materializer) {
        this.minY = settings.generationShapeConfig().minimumY();
        this.maxYExclusive = minY + settings.generationShapeConfig().height();
        this.seaLevel = settings.seaLevel();
        this.defaultBlock = settings.defaultBlock();
        this.defaultFluid = settings.defaultFluid();
        this.materializer = materializer;
    }

    /**
     * Reconciles all vanilla noise-filled columns with the engine surface.
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
                int targetSurfaceY = materializer.solidSurfaceY(
                        sample, minY, maxYExclusive);
                int sealBottomY = Math.max(minY + 1, targetSurfaceY - SURFACE_SEAL_DEPTH + 1);
                int waterTopExclusive = materializer.waterTopExclusive(
                        sample, seaLevel, minY, maxYExclusive);

                for (int y = minY; y < maxYExclusive; y++) {
                    BlockState state = reconciledState(
                            source,
                            y,
                            sourceSurfaceY,
                            targetSurfaceY,
                            sealBottomY,
                            waterTopExclusive);
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
        return minY;
    }

    private BlockState reconciledState(
            BlockState[] source,
            int y,
            int sourceSurfaceY,
            int targetSurfaceY,
            int sealBottomY,
            int waterTopExclusive) {
        if (y == minY) {
            BlockState floor = source[0];
            return floor == null || floor.isAir()
                    ? Blocks.BEDROCK.getDefaultState()
                    : floor;
        }

        if (y > targetSurfaceY) {
            return y < waterTopExclusive ? defaultFluid : Blocks.AIR.getDefaultState();
        }

        // Always provide a stable solid skin for the engine surface. Vanilla's
        // later surface-rule and carver stages are responsible for turning this
        // into biome material and natural cave entrances.
        if (y >= sealBottomY) {
            return defaultBlock;
        }

        // Raised terrain receives new solid substrate only above the original
        // vanilla surface. Nothing below is shifted: caves, deepslate, ore-vein
        // substrate and aquifers retain their absolute world Y coordinates.
        if (y > sourceSurfaceY) {
            return defaultBlock;
        }

        BlockState state = source[y - minY];
        return state == null ? defaultBlock : state;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
