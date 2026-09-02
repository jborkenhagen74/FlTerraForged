package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.WaterDecorationContext;
import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.core.biome.BiomeRole;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.RiparianZone;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

/** Version-bound post-feature decorator for natural Minecraft 1.20.1 watercourses. */
final class WatercourseDecorator {

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    };

    private static final long SEAGRASS_SALT = 0x1892C6F04DB73A5EL;
    private static final long LILY_SALT = 0xD7A20B5E4C91386FL;
    private static final long BANK_PLANT_SALT = 0x5C8E21A7D4903BF6L;
    private static final long LAND_PLANT_SALT = 0x6A31D942E5B708CFL;
    private static final long LAND_VARIANT_SALT = 0xB20F81C46D73A95EL;
    private static final long STAIR_SALT = 0x83F1D46A20B79CE5L;
    private static final long SPRAY_SALT = 0x31B8E5C792A40DF6L;
    private static final long DAM_SALT = 0xA17C58D3E60942BFL;

    private final VanillaBlockMaterializer materializer;
    private final boolean enabled;
    private final boolean plants;
    private final boolean landPlants;
    private final boolean partialBlocks;
    private final boolean spray;
    private final boolean dams;

    /**
     * Creates the family-local decorator from validated materializer options.
     *
     * @param materializer active standard materializer
     * @param options materializer configuration
     */
    WatercourseDecorator(
            VanillaBlockMaterializer materializer,
            Map<String, String> options) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.enabled = option(options, "decoration.enabled", true);
        this.plants = option(options, "decoration.plants", true);
        this.landPlants = option(options, "decoration.land_plants", true);
        this.partialBlocks = option(options, "decoration.partial_blocks", true);
        this.spray = option(options, "decoration.spray", true);
        this.dams = option(options, "decoration.dams", true);
    }

    /**
     * Applies deterministic habitat clusters and rare bounded structures to one completed chunk.
     *
     * @param context writable completed chunk and semantic terrain view
     */
    void decorate(WaterDecorationContext context) {
        if (!enabled) {
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
                if (isAquatic(sample)) {
                    decorateAquatic(context, mutable, x, z, sample);
                } else if (isBank(sample)) {
                    decorateBank(context, mutable, x, z, sample);
                } else {
                    decorateLand(context, mutable, x, z, sample);
                }
            }
        }

        if (dams) {
            decorateDams(context, mutable);
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
        if (waterDepth < 1) {
            return;
        }

        if (partialBlocks && waterDepth >= 2 && shouldPlaceStair(sample, x, z)) {
            Direction facing = bedFacing(context.terrainWorld(), x, z);
            BlockState stair = stairState(sample, bedY, facing);
            mutable.set(x, bedY, z);
            if (stair.canPlaceAt(context.world(), mutable)) {
                context.chunk().setBlockState(mutable, stair, false);
            }
        }

        if (plants && waterDepth >= 2 && waterDepth <= 8 && sample.slope() < 0.90D) {
            double habitat = NaturalMaterialField.sample(x, z, SEAGRASS_SALT, 38.0D);
            boolean plantAnchor = habitat > 0.48D
                    && NaturalMaterialField.sparse(x, z, SEAGRASS_SALT, 2, habitat);
            pruneAquaticPlant(context, mutable, x, bedY + 1, z, sample, plantAnchor);
            if (plantAnchor) {
                if (waterDepth >= 3 && habitat > 0.68D) {
                    placeTallSeagrass(context, mutable, x, bedY + 1, z, sample);
                } else {
                    placeReplacingFluid(
                            context,
                            mutable,
                            x,
                            bedY + 1,
                            z,
                            Blocks.SEAGRASS.getDefaultState(),
                            sample);
                }
            }
        }

        if (plants
                && StandardTerrainTypes.LAKE.equals(sample.terrainType())
                && waterDepth >= 2
                && moist(sample) > 0.58D
                && NaturalMaterialField.sparse(x, z, LILY_SALT, 5, 0.54D)) {
            placeIfAir(
                    context,
                    mutable,
                    x,
                    waterTop,
                    z,
                    Blocks.LILY_PAD.getDefaultState());
        }

        if (spray) {
            decorateSpray(context, mutable, x, z, sample, waterTop);
        }
    }

    private void decorateBank(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample) {
        if (!plants) {
            return;
        }
        int y = materializer.solidSurfaceY(sample) + 1;
        double moisture = moist(sample);
        double strength = StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                ? RiparianZone.lakeShoreStrength(sample)
                : RiparianZone.bankStrength(sample);
        double habitat = NaturalMaterialField.sample(x, z, BANK_PLANT_SALT, 34.0D);
        double carpetThreshold = 0.28D + strength * 0.38D;
        if (partialBlocks && moisture > 0.44D && habitat < carpetThreshold) {
            placeIfAir(
                    context,
                    mutable,
                    x,
                    y,
                    z,
                    Blocks.MOSS_CARPET.getDefaultState());
            return;
        }
        double plantDensity = Math.min(0.94D, 0.24D + strength * 0.68D);
        if (!NaturalMaterialField.sparse(x, z, BANK_PLANT_SALT, 3, plantDensity)) {
            return;
        }
        BlockState plant;
        if (sample.climate().isAvailable()
                && sample.climate().temperature() > 0.78D
                && moisture > 0.68D) {
            plant = Blocks.BAMBOO.getDefaultState();
        } else if (isBesideWater(context.terrainWorld(), x, z) && moisture > 0.42D) {
            plant = Blocks.SUGAR_CANE.getDefaultState();
        } else if (moisture > 0.55D) {
            plant = Blocks.FERN.getDefaultState();
        } else {
            plant = Blocks.GRASS.getDefaultState();
        }
        placeIfAir(context, mutable, x, y, z, plant);
    }

    private void decorateLand(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample) {
        if (!plants
                || !landPlants
                || sample.surfaceHeight() < 60.0D
                || sample.surfaceHeight() > 140.0D
                || sample.slope() > 0.82D) {
            return;
        }
        BiomeRole role = BiomeClimateRouter.route(sample);
        double density = landPlantDensity(role, moist(sample));
        if (!(density > 0.0D)) {
            return;
        }
        double habitat = NaturalMaterialField.sample(x, z, LAND_PLANT_SALT, 56.0D);
        if (habitat > density
                || !NaturalMaterialField.sparse(
                        x,
                        z,
                        LAND_PLANT_SALT,
                        3,
                        0.48D + density * 0.46D)) {
            return;
        }
        int y = materializer.solidSurfaceY(sample) + 1;
        double variant = NaturalMaterialField.sample(x, z, LAND_VARIANT_SALT, 31.0D);
        placeIfAir(context, mutable, x, y, z, landPlant(role, moist(sample), variant));
    }

    private static double landPlantDensity(BiomeRole role, double moisture) {
        double climateFactor = 0.72D + moisture * 0.28D;
        double density = switch (role) {
            case WETLAND -> 0.86D;
            case TEMPERATE_DENSE_FOREST, HOT_WET -> 0.78D;
            case COOL_FOREST, TEMPERATE_FOREST, BOREAL_FOREST -> 0.72D;
            case TEMPERATE_OPEN_WOODLAND, MEDITERRANEAN_WOODLAND -> 0.66D;
            case TEMPERATE_GRASSLAND, COOL_GRASSLAND, ALPINE_MEADOW -> 0.61D;
            case MEDITERRANEAN_GRASSLAND, HOT_SEASONAL -> 0.42D;
            case POLAR_PLAIN, ALPINE_ROCK, HOT_DRY,
                    OCEAN_COLD, OCEAN_TEMPERATE, OCEAN_WARM,
                    OCEAN_DEEP_COLD, OCEAN_DEEP_TEMPERATE, OCEAN_DEEP_WARM,
                    COAST_SANDY, COAST_ROCKY, RIVER_COLD, RIVER_TEMPERATE -> 0.0D;
        };
        return density * climateFactor;
    }

    private static BlockState landPlant(BiomeRole role, double moisture, double variant) {
        if (role == BiomeRole.WETLAND
                || role == BiomeRole.TEMPERATE_DENSE_FOREST
                || role == BiomeRole.COOL_FOREST
                || role == BiomeRole.BOREAL_FOREST
                || moisture > 0.74D) {
            return variant < 0.72D
                    ? Blocks.FERN.getDefaultState()
                    : Blocks.GRASS.getDefaultState();
        }
        if (role == BiomeRole.ALPINE_MEADOW || role == BiomeRole.TEMPERATE_GRASSLAND) {
            if (variant > 0.82D) {
                return Blocks.CORNFLOWER.getDefaultState();
            }
            if (variant > 0.69D) {
                return Blocks.AZURE_BLUET.getDefaultState();
            }
            if (variant > 0.57D) {
                return Blocks.DANDELION.getDefaultState();
            }
        }
        if (role == BiomeRole.TEMPERATE_OPEN_WOODLAND && variant > 0.80D) {
            return Blocks.POPPY.getDefaultState();
        }
        return variant < 0.24D && moisture > 0.52D
                ? Blocks.FERN.getDefaultState()
                : Blocks.GRASS.getDefaultState();
    }

    private void decorateSpray(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            TerrainSample sample,
            int waterTop) {
        if (!StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || sample.surfaceHeight() <= 120.0D
                || sample.slope() < 0.55D
                || !NaturalMaterialField.sparse(x, z, SPRAY_SALT, 3, 0.62D)) {
            return;
        }
        Direction higher = higherWaterNeighbor(context.terrainWorld(), x, z, waterTop);
        if (higher == null) {
            return;
        }
        placeIfAir(
                context,
                mutable,
                x,
                waterTop,
                z,
                Blocks.COBWEB.getDefaultState());
        int bankX = x + higher.rotateYClockwise().getOffsetX();
        int bankZ = z + higher.rotateYClockwise().getOffsetZ();
        TerrainSample bank = context.terrainWorld().sample(bankX, bankZ);
        if (!isAquatic(bank) && insideChunk(context.chunk(), bankX, bankZ)) {
            placeIfAir(
                    context,
                    mutable,
                    bankX,
                    materializer.solidSurfaceY(bank) + 1,
                    bankZ,
                    Blocks.WHITE_CARPET.getDefaultState());
        }
    }

    private void decorateDams(WaterDecorationContext context, BlockPos.Mutable mutable) {
        ChunkPos chunkPos = context.chunk().getPos();
        for (int localZ = 4; localZ < 12; localZ++) {
            int z = chunkPos.getStartZ() + localZ;
            for (int localX = 4; localX < 12; localX++) {
                int x = chunkPos.getStartX() + localX;
                if (!NaturalMaterialField.sparse(x, z, DAM_SALT, 32, 0.18D)) {
                    continue;
                }
                TerrainSample sample = context.terrainWorld().sample(x, z);
                if (!damHabitat(sample) || sample.river().distance() > 1.25D) {
                    continue;
                }
                Direction crossing = crossChannelDirection(context.terrainWorld(), x, z);
                int halfWidth = Math.max(1, Math.min(
                        3,
                        (int) Math.ceil(sample.river().width() * 0.38D)));
                if (validDamSpan(context.terrainWorld(), x, z, crossing, halfWidth)) {
                    placeDam(context, mutable, x, z, crossing, halfWidth);
                }
            }
        }
    }

    private boolean validDamSpan(
            TerrainWorld terrain,
            int x,
            int z,
            Direction crossing,
            int halfWidth) {
        int referenceTop = materializer.waterTopExclusive(terrain.sample(x, z));
        for (int offset = -halfWidth; offset <= halfWidth; offset++) {
            TerrainSample sample = terrain.sample(
                    x + crossing.getOffsetX() * offset,
                    z + crossing.getOffsetZ() * offset);
            if (!materializer.hasMaterializedWater(sample)
                    || Math.abs(materializer.waterTopExclusive(sample) - referenceTop) > 1) {
                return false;
            }
        }
        return true;
    }

    private void placeDam(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int z,
            Direction crossing,
            int halfWidth) {
        TerrainSample center = context.terrainWorld().sample(x, z);
        int waterTop = materializer.waterTopExclusive(center);
        BlockState log = Blocks.OAK_LOG.getDefaultState().with(Properties.AXIS, crossing.getAxis());
        BlockState fence = Blocks.OAK_FENCE.getDefaultState().with(Properties.WATERLOGGED, true);
        for (int offset = -halfWidth; offset <= halfWidth; offset++) {
            int px = x + crossing.getOffsetX() * offset;
            int pz = z + crossing.getOffsetZ() * offset;
            mutable.set(px, waterTop - 1, pz);
            context.chunk().setBlockState(mutable, log, false);
            if (Math.floorMod(offset, 2) == 0) {
                mutable.set(px, waterTop, pz);
                if (context.chunk().getBlockState(mutable).isAir()
                        && fence.canPlaceAt(context.world(), mutable)) {
                    context.chunk().setBlockState(mutable, fence.with(Properties.WATERLOGGED, false), false);
                }
            }
            if (Math.abs(offset) == halfWidth) {
                mutable.set(px, waterTop - 2, pz);
                context.chunk().setBlockState(mutable, Blocks.MUD.getDefaultState(), false);
            }
        }
    }

    private boolean shouldPlaceStair(TerrainSample sample, int x, int z) {
        if (sample.slope() < 0.32D && sample.surfaceHeight() < 90.0D) {
            return false;
        }
        double formation = NaturalMaterialField.sample(x, z, STAIR_SALT, 28.0D);
        return formation > 0.70D && formation < 0.84D;
    }

    private static BlockState stairState(
            TerrainSample sample,
            int y,
            Direction facing) {
        Block block;
        if (y >= 90) {
            block = sample.slope() > 0.90D ? Blocks.ANDESITE_STAIRS : Blocks.COBBLESTONE_STAIRS;
        } else if (sample.climate().isAvailable()
                && sample.climate().temperature() > 0.68D
                && sample.climate().moisture() < 0.38D) {
            block = Blocks.SANDSTONE_STAIRS;
        } else {
            block = Blocks.COBBLESTONE_STAIRS;
        }
        return block.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, facing)
                .with(Properties.WATERLOGGED, true);
    }

    private Direction bedFacing(TerrainWorld terrain, int x, int z) {
        int west = materializer.solidSurfaceY(terrain.sample(x - 1, z));
        int east = materializer.solidSurfaceY(terrain.sample(x + 1, z));
        int north = materializer.solidSurfaceY(terrain.sample(x, z - 1));
        int south = materializer.solidSurfaceY(terrain.sample(x, z + 1));
        int dx = east - west;
        int dz = south - north;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.WEST : Direction.EAST;
        }
        return dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private static Direction crossChannelDirection(TerrainWorld terrain, int x, int z) {
        double dx = terrain.sample(x + 1, z).river().distance()
                - terrain.sample(x - 1, z).river().distance();
        double dz = terrain.sample(x, z + 1).river().distance()
                - terrain.sample(x, z - 1).river().distance();
        return Math.abs(dx) >= Math.abs(dz) ? Direction.EAST : Direction.SOUTH;
    }

    private Direction higherWaterNeighbor(TerrainWorld terrain, int x, int z, int waterTop) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            TerrainSample neighbor = terrain.sample(
                    x + direction.getOffsetX(),
                    z + direction.getOffsetZ());
            if (isAquatic(neighbor)
                    && materializer.waterTopExclusive(neighbor) == waterTop + 1) {
                return direction;
            }
        }
        return null;
    }

    private boolean isBesideWater(TerrainWorld terrain, int x, int z) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (isAquatic(terrain.sample(
                    x + direction.getOffsetX(),
                    z + direction.getOffsetZ()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isAquatic(TerrainSample sample) {
        if (materializer.hasMaterializedWater(sample)) {
            return true;
        }
        return (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                        || StandardTerrainTypes.COAST.equals(sample.terrainType()))
                && materializer.waterTopExclusive(sample) > materializer.solidSurfaceTop(sample);
    }

    private static boolean isBank(TerrainSample sample) {
        return StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                || RiparianZone.isRiverBank(sample);
    }

    private static boolean damHabitat(TerrainSample sample) {
        if (!StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || !sample.river().hasFlow()
                || sample.river().flow() < 3.5D
                || sample.river().flow() > 18.0D
                || sample.river().width() < 3.0D
                || sample.river().width() > 8.0D
                || sample.surfaceHeight() < 60.0D
                || sample.surfaceHeight() > 89.0D
                || sample.slope() > 0.65D
                || !sample.climate().isAvailable()) {
            return false;
        }
        return sample.climate().moisture() > 0.54D
                && sample.climate().temperature() > 0.22D
                && sample.climate().temperature() < 0.78D;
    }

    private void placeReplacingFluid(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            BlockState state,
            TerrainSample sample) {
        if (!insideChunk(context.chunk(), x, z)) {
            return;
        }
        mutable.set(x, y, z);
        if (context.chunk().getBlockState(mutable).equals(materializer.fluidState(sample))
                && state.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, state, false);
        }
    }

    private void placeTallSeagrass(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            TerrainSample sample) {
        if (!insideChunk(context.chunk(), x, z)) {
            return;
        }
        BlockState fluid = materializer.fluidState(sample);
        BlockState lower = Blocks.TALL_SEAGRASS.getDefaultState()
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upper = Blocks.TALL_SEAGRASS.getDefaultState()
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        mutable.set(x, y, z);
        if (!context.chunk().getBlockState(mutable).equals(fluid)
                || !lower.canPlaceAt(context.world(), mutable)) {
            return;
        }
        mutable.set(x, y + 1, z);
        if (!context.chunk().getBlockState(mutable).equals(fluid)) {
            return;
        }
        mutable.set(x, y, z);
        context.chunk().setBlockState(mutable, lower, false);
        mutable.set(x, y + 1, z);
        if (upper.canPlaceAt(context.world(), mutable)) {
            context.chunk().setBlockState(mutable, upper, false);
        } else {
            mutable.set(x, y, z);
            context.chunk().setBlockState(mutable, fluid, false);
        }
    }

    private void pruneAquaticPlant(
            WaterDecorationContext context,
            BlockPos.Mutable mutable,
            int x,
            int y,
            int z,
            TerrainSample sample,
            boolean keep) {
        if (keep || !insideChunk(context.chunk(), x, z)) {
            return;
        }
        BlockState fluid = materializer.fluidState(sample);
        mutable.set(x, y, z);
        BlockState lower = context.chunk().getBlockState(mutable);
        if (lower.isOf(Blocks.SEAGRASS)) {
            context.chunk().setBlockState(mutable, fluid, false);
            return;
        }
        if (!lower.isOf(Blocks.TALL_SEAGRASS)) {
            return;
        }
        context.chunk().setBlockState(mutable, fluid, false);
        mutable.set(x, y + 1, z);
        if (context.chunk().getBlockState(mutable).isOf(Blocks.TALL_SEAGRASS)) {
            context.chunk().setBlockState(mutable, fluid, false);
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

    private static double moist(TerrainSample sample) {
        return sample.climate().isAvailable() ? sample.climate().moisture() : 0.5D;
    }

    private static boolean insideChunk(Chunk chunk, int x, int z) {
        ChunkPos pos = chunk.getPos();
        return x >= pos.getStartX()
                && x < pos.getStartX() + 16
                && z >= pos.getStartZ()
                && z < pos.getStartZ() + 16;
    }

    private static boolean option(
            Map<String, String> options,
            String key,
            boolean fallback) {
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
