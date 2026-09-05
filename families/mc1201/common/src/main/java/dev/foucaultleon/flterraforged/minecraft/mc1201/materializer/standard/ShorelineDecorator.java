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
 * <p>The decorator deliberately works in patches rather than per-block random noise. A shoreline
 * can therefore alternate between bare sediment, low vegetation, lush shrub sections and rocky
 * banks. Climate controls which contents are plausible, while the patch field controls where each
 * style occurs. It is part of the standard materializer only; external providers remain free to
 * implement their own geometry- and palette-aware decoration.</p>
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
        double style = NaturalMaterialField.sample(x, z, STYLE_SALT, 64.0D);
        double detail = NaturalMaterialField.sample(x, z, DETAIL_SALT, 21.0D);
        double moisture = sample.climate().isAvailable()
                ? clamp01(sample.climate().moisture())
                : 0.5D;

        if (style < 0.23D) {
            pruneSoftVegetation(context, mutable, x, y, z);
            return;
        }

        boolean rocky = style > 0.77D || sample.slope() > 0.72D;
        if (rocky && rocks) {
            pruneSoftVegetation(context, mutable, x, y, z);
            if (NaturalMaterialField.sparse(x, z, ROCK_SALT, 5, 0.48D + strength * 0.24D)) {
                BlockState rock = moisture > 0.58D && detail > 0.52D
                        ? Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState()
                        : Blocks.COBBLESTONE_WALL.getDefaultState();
                placeIfAir(context, mutable, x, y, z, rock);
            }
            return;
        }

        if (!shrubs) {
            return;
        }
        if (moisture < 0.24D) {
            if (NaturalMaterialField.sparse(x, z, SHRUB_SALT, 7, 0.42D * strength)) {
                placeIfAir(context, mutable, x, y, z, Blocks.DEAD_BUSH.getDefaultState());
            }
            return;
        }

        if (style > 0.48D && moisture > 0.52D) {
            if (!NaturalMaterialField.sparse(x, z, SHRUB_SALT, 4, 0.46D + strength * 0.34D)) {
                return;
            }
            BlockState shrub;
            if (moisture > 0.72D && detail > 0.58D) {
                shrub = Blocks.AZALEA.getDefaultState();
            } else if (moisture > 0.60D) {
                shrub = Blocks.FERN.getDefaultState();
            } else {
                shrub = Blocks.GRASS.getDefaultState();
            }
            placeIfAir(context, mutable, x, y, z, shrub);
        }
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

    private static void pruneSoftVegetation(
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
