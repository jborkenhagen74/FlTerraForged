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
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

/**
 * Adds broad, deterministic shoreline habitat sections after ordinary watercourse decoration.
 *
 * <p>R48 keeps broad habitat patches but makes vegetated shoreline the normal case. Bare sections
 * are deliberately rare, while wet and temperate banks receive continuous low groundcover plus
 * clustered grasses, ferns, reeds and shrubs. Rocky sections use low slabs and substrate patches
 * instead of wall blocks, so the result reads as natural stones rather than placed posts.</p>
 *
 * <p>This decorator belongs only to the standard materializer. External materializers retain full
 * control over their own block palettes and geometry-aware decoration.</p>
 */
final class ShorelineDecorator {

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    };

    private static final long STYLE_SALT = 0x6F2E91B4C37DA805L;
    private static final long DETAIL_SALT = 0xB134A8E2D76F509CL;
    private static final long ROCK_SALT = 0x3D8C6AF129E750B4L;
    private static final long SHRUB_SALT = 0x9A51E603C2B87D4FL;
    private static final long GROUNDCOVER_SALT = 0x73C419EA0D65B28FL;
    private static final long REED_SALT = 0xC50D34A817E96B2FL;
    private static final long FLOWER_SALT = 0x2B9F40C61D783AE5L;

    private final VanillaBlockMaterializer materializer;
    private final boolean enabled;
    private final boolean rocks;
    private final boolean shrubs;

    ShorelineDecorator(VanillaBlockMaterializer materializer, Map<String, String> options) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.enabled = option(options, "decoration.shoreline", true);
        this.rocks = option(options, "decoration.shoreline_rocks", true);
        this.shrubs = option(options, "decoration.shoreline_shrubs", true);
    }

    void decorate(WaterDecorationContext context) {
        if (!enabled) {
            return;
        }
        Chunk chunk = context.chunk();
        ChunkPos chunkPos = chunk.getPos();
        TerrainWorld terrain = context.terrainWorld();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; localZ++) {
            int z = chunkPos.getStartZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int x = chunkPos.getStartX() + localX;
                TerrainSample sample = terrain.sample(x, z);
                if (!isShore(terrain, x, z, sample)) {
                    continue;
                }
                decorateColumn(context, mutable, x, z, sample);
            }
        }
    }

    private void decorateColumn(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample) {
        int y = materializer.solidSurfaceY(sample) + 1;
        double strength = shoreStrength(sample);
        double style = NaturalMaterialField.sample(x, z, STYLE_SALT, 72.0D);
        double detail = NaturalMaterialField.sample(x, z, DETAIL_SALT, 19.0D);
        double moisture = sample.climate().isAvailable()
                ? clamp01(sample.climate().moisture())
                : 0.5D;
        double lushness = clamp01(moisture * 0.72D + strength * 0.28D);

        boolean rocky = style > 0.88D || sample.slope() > 0.88D;
        if (rocky && rocks) {
            decorateRock(context, mutable, x, y, z, moisture, detail, strength);
        }

        // Truly bare shore is intentionally uncommon. Even rocky banks retain plants between rocks.
        if (style < 0.08D && moisture < 0.30D) {
            if (NaturalMaterialField.sparse(x, z, SHRUB_SALT, 4, 0.42D + strength * 0.18D)) {
                placeIfAir(context, mutable, x, y, z, Blocks.DEAD_BUSH.getDefaultState());
            }
            return;
        }

        if (moisture > 0.38D
                && NaturalMaterialField.sparse(
                        x,
                        z,
                        GROUNDCOVER_SALT,
                        2,
                        0.62D + lushness * 0.32D)) {
            placeIfAir(context, mutable, x, y, z, Blocks.MOSS_CARPET.getDefaultState());
        }

        if (moisture > 0.40D
                && isBesideWater(context.terrainWorld(), x, z)
                && NaturalMaterialField.sparse(
                        x,
                        z,
                        REED_SALT,
                        4,
                        0.36D + lushness * 0.36D)) {
            placeIfAir(context, mutable, x, y, z, Blocks.SUGAR_CANE.getDefaultState());
        }

        if (!shrubs) {
            return;
        }

        if (moisture < 0.22D) {
            if (NaturalMaterialField.sparse(x, z, SHRUB_SALT, 4, 0.50D + strength * 0.22D)) {
                placeIfAir(context, mutable, x, y, z, Blocks.DEAD_BUSH.getDefaultState());
            }
            return;
        }

        int spacing = lushness > 0.68D ? 2 : 3;
        double habitat = 0.56D + lushness * 0.38D;
        if (rocky) {
            habitat *= 0.74D;
        }
        if (NaturalMaterialField.sparse(x, z, SHRUB_SALT, spacing, habitat)) {
            BlockState plant = choosePlant(moisture, lushness, detail);
            placeIfAir(context, mutable, x, y, z, plant);
        }

        if (lushness > 0.72D
                && detail > 0.38D
                && NaturalMaterialField.sparse(
                        x,
                        z,
                        FLOWER_SALT,
                        5,
                        0.34D + lushness * 0.24D)) {
            BlockState flower = detail > 0.66D
                    ? Blocks.POPPY.getDefaultState()
                    : Blocks.DANDELION.getDefaultState();
            placeIfAir(context, mutable, x, y, z, flower);
        }
    }

    private void decorateRock(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            double moisture,
            double detail,
            double strength) {
        double patch = NaturalMaterialField.sample(x, z, ROCK_SALT, 23.0D);
        if (patch > 0.66D) {
            replaceSurfaceRock(context, mutable, x, y - 1, z, moisture, detail);
        }
        if (patch < 0.76D
                || !NaturalMaterialField.sparse(
                        x,
                        z,
                        ROCK_SALT,
                        3,
                        0.34D + strength * 0.30D)) {
            return;
        }

        clearSoftPlant(context, mutable, x, y, z);
        BlockState rock;
        if (moisture > 0.62D && detail > 0.50D) {
            rock = Blocks.MOSSY_COBBLESTONE_SLAB.getDefaultState();
        } else if (detail > 0.58D) {
            rock = Blocks.ANDESITE_SLAB.getDefaultState();
        } else {
            rock = Blocks.COBBLESTONE_SLAB.getDefaultState();
        }
        placeIfAir(context, mutable, x, y, z, rock);
    }

    private static BlockState choosePlant(
            double moisture,
            double lushness,
            double detail) {
        if (lushness > 0.84D && detail > 0.72D) {
            return Blocks.FLOWERING_AZALEA.getDefaultState();
        }
        if (lushness > 0.76D && detail > 0.56D) {
            return Blocks.AZALEA.getDefaultState();
        }
        if (moisture > 0.54D) {
            return Blocks.FERN.getDefaultState();
        }
        return Blocks.GRASS.getDefaultState();
    }

    private static void replaceSurfaceRock(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            double moisture,
            double detail) {
        if (!insideChunk(context.chunk(), x, z)) {
            return;
        }
        mutable.set(x, y, z);
        BlockState current = context.chunk().getBlockState(mutable);
        if (current.isAir() || !current.getFluidState().isEmpty()) {
            return;
        }
        BlockState rock;
        if (moisture > 0.64D && detail > 0.56D) {
            rock = Blocks.MOSSY_COBBLESTONE.getDefaultState();
        } else if (detail > 0.52D) {
            rock = Blocks.ANDESITE.getDefaultState();
        } else {
            rock = Blocks.GRAVEL.getDefaultState();
        }
        context.chunk().setBlockState(mutable, rock, false);
    }

    private boolean isShore(TerrainWorld terrain, int x, int z, TerrainSample sample) {
        if (StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                || RiparianZone.isRiverBank(sample)) {
            return true;
        }
        return StandardTerrainTypes.COAST.equals(sample.terrainType())
                && !materializer.hasFinalWetEnvelope(sample, x, z)
                && isBesideWater(terrain, x, z);
    }

    private boolean isBesideWater(TerrainWorld terrain, int x, int z) {
        for (Direction direction : HORIZONTAL) {
            int nx = x + direction.getOffsetX();
            int nz = z + direction.getOffsetZ();
            TerrainSample neighbor = terrain.sample(nx, nz);
            if (materializer.hasFinalWetEnvelope(neighbor, nx, nz)) {
                return true;
            }
        }
        return false;
    }

    private static double shoreStrength(TerrainSample sample) {
        if (StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())) {
            return clamp01(RiparianZone.lakeShoreStrength(sample));
        }
        if (RiparianZone.isRiverBank(sample)) {
            return clamp01(RiparianZone.bankStrength(sample));
        }
        return 0.62D;
    }

    private static void clearSoftPlant(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z) {
        if (!insideChunk(context.chunk(), x, z)) {
            return;
        }
        mutable.set(x, y, z);
        BlockState state = context.chunk().getBlockState(mutable);
        if (state.isOf(Blocks.GRASS)
                || state.isOf(Blocks.FERN)
                || state.isOf(Blocks.TALL_GRASS)
                || state.isOf(Blocks.SUGAR_CANE)
                || state.isOf(Blocks.MOSS_CARPET)
                || state.isOf(Blocks.DANDELION)
                || state.isOf(Blocks.POPPY)) {
            context.chunk().setBlockState(mutable, Blocks.AIR.getDefaultState(), false);
        }
    }

    private static void placeIfAir(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state) {
        if (!insideChunk(context.chunk(), x, z)) {
            return;
        }
        mutable.set(x, y, z);
        if (context.chunk().getBlockState(mutable).isAir()
                && state.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, state, false);
        }
    }

    private static boolean insideChunk(Chunk chunk, int x, int z) {
        ChunkPos pos = chunk.getPos();
        return x >= pos.getStartX()
                && x < pos.getStartX() + 16
                && z >= pos.getStartZ()
                && z < pos.getStartZ() + 16;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static boolean option(Map<String, String> options, String key, boolean fallback) {
        String raw = options.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        throw new IllegalArgumentException(
                "Materializer option '" + key + "' must be true or false");
    }
}
