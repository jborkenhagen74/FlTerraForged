package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.WaterDecorationContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.RiparianZone;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Geometry-neutral post-feature decorator for natural Minecraft 1.20.1 watercourses.
 *
 * <p>R62 deliberately limits this stage to vegetation. Earlier revisions could replace river-bed
 * blocks with stairs, construct log dams and add spray markers. Those decorations changed the
 * already finalized hydraulic cross-section and were visually indistinguishable from broken river
 * steps. The Engine now owns every solid and fluid shape; decoration may only replace suitable
 * vegetation/fluid cells and never raises, lowers or blocks a watercourse.</p>
 */
final class WatercourseDecorator {

    private static final long SEAGRASS_SALT = 0x1892C6F04DB73A5EL;
    private static final long LILY_SALT = 0xD7A20B5E4C91386FL;
    private static final long BANK_PLANT_SALT = 0x5C8E21A7D4903BF6L;

    private final VanillaBlockMaterializer materializer;
    private final boolean enabled;
    private final boolean plants;

    /**
     * Creates the family-local decorator from validated materializer options.
     *
     * @param materializer active standard materializer
     * @param options materializer configuration
     */
    WatercourseDecorator(VanillaBlockMaterializer materializer, Map<String, String> options) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.enabled = option(options, "decoration.enabled", true);
        this.plants = option(options, "decoration.plants", true);
    }

    /**
     * Applies deterministic vegetation without changing final terrain or water geometry.
     *
     * @param context writable completed chunk and semantic terrain view
     */
    void decorate(WaterDecorationContext context) {
        if (!enabled || !plants) {
            return;
        }
        Chunk chunk = context.chunk();
        TerrainWorld terrain = context.terrainWorld();
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; localZ++) {
            int z = chunkPos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int x = chunkPos.getStartX() + localX;
                TerrainSample sample = terrain.sample(x, z);
                if (materializer.hasMaterializedWater(sample)) {
                    decorateAquatic(context, mutable, x, z, sample);
                } else if (isBank(sample)) {
                    decorateBank(context, mutable, x, z, sample);
                }
            }
        }
    }

    private void decorateAquatic(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample) {
        int bedY = materializer.solidSurfaceY(sample);
        int waterTop = materializer.waterTopExclusive(sample);
        int waterDepth = waterTop - bedY - 1;
        if (waterDepth < 2) {
            return;
        }

        if (waterDepth <= 8 && sample.slope() < 0.90D) {
            double habitat = NaturalMaterialField.sample(x, z, SEAGRASS_SALT, 38.0D);
            if (habitat > 0.48D
                    && NaturalMaterialField.sparse(x, z, SEAGRASS_SALT, 2, habitat)) {
                placeInWater(
                        context,
                        mutable,
                        x,
                        bedY + 1,
                        z,
                        Blocks.SEAGRASS.getDefaultState());
            }
        }

        if (StandardTerrainTypes.LAKE.equals(sample.terrainType())
                && waterDepth >= 2
                && moist(sample) > 0.58D
                && NaturalMaterialField.sparse(x, z, LILY_SALT, 7, 0.50D)) {
            placeOnWater(
                    context,
                    mutable,
                    x,
                    waterTop,
                    z,
                    Blocks.LILY_PAD.getDefaultState());
        }
    }

    private void decorateBank(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample) {
        int y = materializer.solidSurfaceY(sample) + 1;
        double moisture = moist(sample);
        double strength = StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                ? RiparianZone.lakeShoreStrength(sample)
                : RiparianZone.bankStrength(sample);
        if (!NaturalMaterialField.sparse(x, z, BANK_PLANT_SALT, 6, strength * 0.46D)) {
            return;
        }

        BlockState plant;
        if (moisture > 0.62D) {
            plant = Blocks.FERN.getDefaultState();
        } else {
            plant = Blocks.GRASS.getDefaultState();
        }
        placeOnDrySurface(context, mutable, x, y, z, plant);
    }

    private static void placeInWater(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state) {
        mutable.set(x, y, z);
        if (!context.chunk().getBlockState(mutable).isOf(Blocks.WATER)) {
            return;
        }
        if (state.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, state, false);
        }
    }

    private static void placeOnWater(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state) {
        mutable.set(x, y, z);
        if (!context.chunk().getBlockState(mutable).isAir()) {
            return;
        }
        mutable.setY(y - 1);
        boolean waterBelow = !context.chunk().getFluidState(mutable).isEmpty();
        mutable.setY(y);
        if (waterBelow && state.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, state, false);
        }
    }

    private static void placeOnDrySurface(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state) {
        mutable.set(x, y, z);
        if (!context.chunk().getBlockState(mutable).isAir()) {
            return;
        }
        mutable.setY(y - 1);
        boolean drySupport = context.chunk().getFluidState(mutable).isEmpty();
        mutable.setY(y);
        if (drySupport && state.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, state, false);
        }
    }

    private static boolean isBank(TerrainSample sample) {
        return StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                || RiparianZone.isRiverBank(sample);
    }

    private static double moist(TerrainSample sample) {
        return sample.climate().isAvailable() ? sample.climate().moisture() : 0.5D;
    }

    private static boolean option(Map<String, String> options, String key, boolean fallback) {
        String value = options.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
