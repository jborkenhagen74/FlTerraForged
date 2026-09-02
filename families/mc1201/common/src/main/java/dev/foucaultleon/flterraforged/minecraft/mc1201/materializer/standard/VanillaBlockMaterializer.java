package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerCapabilities;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.WaterDecorationContext;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen.RiparianZone;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/** Standard full-block materializer shipped with FlTerraForged for Minecraft 1.20.1. */
public final class VanillaBlockMaterializer implements BlockMaterializer {

    private static final double MIN_WET_DEPTH = 0.05D;
    private static final MaterializerCapabilities CAPABILITIES =
            new MaterializerCapabilities(1.0D, false, false);

    private final MaterializerContext context;
    private final ConfiguredBlockSet riverBed;
    private final ConfiguredBlockSet lakeBed;
    private final ConfiguredBlockSet coast;
    private final ConfiguredBlockSet lakeShoreDry;
    private final ConfiguredBlockSet lakeShoreWet;
    private final ConfiguredBlockSet riparian;
    private final ConfiguredBlockSet landSurface;
    private final ConfiguredBlockSet landFiller;
    private final ConfiguredBlockSet mountains;
    private final ConfiguredBlockSet plateau;
    private final ConfiguredBlockSet hills;
    private final ConfiguredBlockSet plains;
    private final ConfiguredBlockSet valley;
    private final ConfiguredBlockSet oceanBed;
    private final ConfiguredBlockSet substrate;
    private final ConfiguredBlockSet seal;
    private final WatercourseMaterialPalette watercourses;
    private final MarineMaterialPalette marine;
    private final WatercourseDecorator decorator;
    private final int hydrologyCaveMargin;
    private final int hydrologyBedSealDepth;
    private final int hydrologyBankSealDepth;
    private final int hydrologyGapRepairRadius;

    /**
     * Creates the vanilla-compatible full-block implementation.
     *
     * @param context active generation context
     */
    public VanillaBlockMaterializer(MaterializerContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.riverBed = set("blockset.river_bed", Blocks.GRAVEL.getDefaultState());
        this.lakeBed = set("blockset.lake_bed", Blocks.GRAVEL.getDefaultState());
        this.coast = set("blockset.coast", Blocks.SAND.getDefaultState());
        this.lakeShoreDry = set("blockset.lake_shore_dry", Blocks.SAND.getDefaultState());
        this.lakeShoreWet = set("blockset.lake_shore_wet", Blocks.GRASS_BLOCK.getDefaultState());
        this.riparian = set("blockset.riparian", Blocks.GRASS_BLOCK.getDefaultState());
        this.landSurface = set("blockset.land_surface", Blocks.GRASS_BLOCK.getDefaultState());
        this.landFiller = set("blockset.land_filler", Blocks.DIRT.getDefaultState());
        this.mountains = set("blockset.mountains", Blocks.STONE.getDefaultState());
        this.plateau = set("blockset.plateau", Blocks.GRASS_BLOCK.getDefaultState());
        this.hills = set("blockset.hills", Blocks.GRASS_BLOCK.getDefaultState());
        this.plains = set("blockset.plains", Blocks.GRASS_BLOCK.getDefaultState());
        this.valley = set("blockset.valley", Blocks.GRASS_BLOCK.getDefaultState());
        this.oceanBed = set("blockset.ocean_bed", Blocks.SAND.getDefaultState());
        this.substrate = set("blockset.substrate", context.defaultBlock());
        this.seal = set("blockset.seal", context.defaultBlock());
        this.watercourses = new WatercourseMaterialPalette(context.options());
        this.marine = new MarineMaterialPalette(context.options());
        this.decorator = new WatercourseDecorator(this, context.options());
        this.hydrologyCaveMargin = integerOption("hydrology.cave_margin", 6, 0, 16);
        this.hydrologyBedSealDepth = integerOption("hydrology.bed_seal_depth", 5, 1, 16);
        this.hydrologyBankSealDepth = integerOption("hydrology.bank_seal_depth", 8, 1, 24);
        this.hydrologyGapRepairRadius = integerOption("hydrology.gap_repair_radius", 2, 0, 4);
    }

    @Override
    public MaterializerContext context() {
        return context;
    }

    @Override
    public MaterializerCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public int solidSurfaceY(TerrainSample sample) {
        int surfaceY = clamp(
                (int) Math.floor(sample.surfaceHeight()),
                context.minY() + 1,
                context.maxYExclusive() - 2);
        RiverSample hydrology = sample.river();
        if (StandardTerrainTypes.LAKE.equals(sample.terrainType())
                && hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH) {
            int waterTopExclusive = clamp(
                    (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                    context.minY() + 1,
                    context.maxYExclusive());
            surfaceY = Math.min(surfaceY, waterTopExclusive - 2);
        }
        return clamp(surfaceY, context.minY() + 1, context.maxYExclusive() - 2);
    }

    @Override
    public int waterTopExclusive(TerrainSample sample) {
        int solidTop = solidSurfaceTop(sample);
        TerrainType terrain = sample.terrainType();
        int waterTop = StandardTerrainTypes.OCEAN.equals(terrain)
                        || StandardTerrainTypes.COAST.equals(terrain)
                ? Math.max(solidTop, context.seaLevel() + 1)
                : solidTop;
        RiverSample hydrology = sample.river();
        if (hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > MIN_WET_DEPTH
                && hydrology.waterSurfaceHeight() > sample.surfaceHeight()) {
            int localWaterTop = (int) Math.floor(hydrology.waterSurfaceHeight()) + 1;
            waterTop = Math.max(waterTop, localWaterTop);
        }
        return clamp(waterTop, context.minY(), context.maxYExclusive());
    }

    @Override
    public boolean hasMaterializedWater(TerrainSample sample) {
        RiverSample hydrology = sample.river();
        if (!hydrology.hasWaterSurfaceHeight() || hydrology.depth() <= MIN_WET_DEPTH) {
            return false;
        }
        int bedTop = solidSurfaceTop(sample);
        int waterTop = clamp(
                (int) Math.floor(hydrology.waterSurfaceHeight()) + 1,
                context.minY(),
                context.maxYExclusive());
        return waterTop > bedTop;
    }

    @Override
    public BlockState bedrockState(TerrainSample sample) {
        return Blocks.BEDROCK.getDefaultState();
    }

    @Override
    public BlockState substrateState(TerrainSample sample) {
        return substrate.choose(sample);
    }

    @Override
    public BlockState surfaceSealState(TerrainSample sample) {
        return seal.choose(sample);
    }

    @Override
    public BlockState airState(TerrainSample sample) {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public BlockState fluidState(TerrainSample sample) {
        return context.defaultFluid();
    }

    @Override
    public BlockState composedTopState(TerrainSample sample) {
        return composedTopState(sample, 0, 0);
    }

    @Override
    public BlockState composedTopState(TerrainSample sample, int x, int z) {
        return forcedSurfaceState(sample, x, z)
                .orElseGet(() -> fallbackSurfaceState(sample, x, z));
    }

    @Override
    public BlockState fillerState(TerrainSample sample) {
        return fillerState(sample, 0, (int) Math.floor(sample.surfaceHeight()) - 1, 0);
    }

    @Override
    public BlockState fillerState(TerrainSample sample, int x, int y, int z) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.RIVER.equals(terrain)) {
            if (hasMaterializedWater(sample)) {
                return riverBed.isConfigured()
                        ? riverBed.choose(sample, x, y, z)
                        : watercourses.bed(sample, x, y, z);
            }
            return watercourses.bankFiller(sample, x, y, z);
        }
        if (StandardTerrainTypes.LAKE.equals(terrain)) {
            if (hasMaterializedWater(sample)) {
                return lakeBed.isConfigured()
                        ? lakeBed.choose(sample, x, y, z)
                        : watercourses.bed(sample, x, y, z);
            }
            return watercourses.bankFiller(sample, x, y, z);
        }
        if (RiparianZone.isDryBank(sample) || RiparianZone.isRiverBank(sample)) {
            return watercourses.bankFiller(sample, x, y, z);
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(terrain)) {
            return watercourses.bankFiller(sample, x, y, z);
        }
        if (StandardTerrainTypes.OCEAN.equals(terrain)) {
            return oceanBed.isConfigured()
                    ? oceanBed.choose(sample, x, y, z)
                    : marine.bed(sample, x, y, z, context.seaLevel());
        }
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return waterTopExclusive(sample) > solidSurfaceTop(sample)
                    ? oceanBed.isConfigured()
                            ? oceanBed.choose(sample, x, y, z)
                            : marine.bed(sample, x, y, z, context.seaLevel())
                    : coast.choose(sample, x, y, z);
        }
        return landFiller.choose(sample, x, y, z);
    }

    @Override
    public Optional<BlockState> forcedSurfaceState(TerrainSample sample) {
        return forcedSurfaceState(sample, 0, 0);
    }

    @Override
    public Optional<BlockState> forcedSurfaceState(TerrainSample sample, int x, int z) {
        TerrainType terrain = sample.terrainType();
        int y = solidSurfaceY(sample);
        if (StandardTerrainTypes.RIVER.equals(terrain) && hasMaterializedWater(sample)) {
            return Optional.of(riverBed.isConfigured()
                    ? riverBed.choose(sample, x, y, z)
                    : watercourses.bed(sample, x, y, z));
        }
        if (StandardTerrainTypes.LAKE.equals(terrain) && hasMaterializedWater(sample)) {
            return Optional.of(lakeBed.isConfigured()
                    ? lakeBed.choose(sample, x, y, z)
                    : watercourses.bed(sample, x, y, z));
        }
        if (StandardTerrainTypes.LAKE_SHORE.equals(terrain)) {
            if (watercourses.isWetBank(sample)) {
                return Optional.of(lakeShoreWet.isConfigured()
                        ? lakeShoreWet.choose(sample, x, y, z)
                        : watercourses.wetBank(sample, x, y, z));
            }
            if (watercourses.coversTransition(sample, x, z)) {
                return Optional.of(lakeShoreDry.isConfigured()
                        ? lakeShoreDry.choose(sample, x, y, z)
                        : watercourses.dryBank(sample, x, y, z));
            }
            return Optional.empty();
        }
        if (RiparianZone.isDryBank(sample) || RiparianZone.isRiverBank(sample)) {
            if (!watercourses.isWetBank(sample)
                    && !watercourses.coversTransition(sample, x, z)) {
                return Optional.empty();
            }
            if (riparian.isConfigured()) {
                return Optional.of(riparian.choose(sample, x, y, z));
            }
            return Optional.of(watercourses.isWetBank(sample)
                    ? watercourses.wetBank(sample, x, y, z)
                    : watercourses.dryBank(sample, x, y, z));
        }
        if (StandardTerrainTypes.OCEAN.equals(terrain)
                || (StandardTerrainTypes.COAST.equals(terrain)
                        && waterTopExclusive(sample) > solidSurfaceTop(sample))) {
            return Optional.of(oceanBed.isConfigured()
                    ? oceanBed.choose(sample, x, y, z)
                    : marine.bed(sample, x, y, z, context.seaLevel()));
        }
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return Optional.of(coast.choose(sample, x, y, z));
        }
        if (StandardTerrainTypes.MOUNTAINS.equals(terrain) && mountains.isConfigured()) {
            return Optional.of(mountains.choose(sample, x, y, z));
        }
        if (StandardTerrainTypes.PLATEAU.equals(terrain) && plateau.isConfigured()) {
            return Optional.of(plateau.choose(sample, x, y, z));
        }
        if (StandardTerrainTypes.HILLS.equals(terrain) && hills.isConfigured()) {
            return Optional.of(hills.choose(sample, x, y, z));
        }
        if (StandardTerrainTypes.PLAINS.equals(terrain) && plains.isConfigured()) {
            return Optional.of(plains.choose(sample, x, y, z));
        }
        if (StandardTerrainTypes.VALLEY.equals(terrain) && valley.isConfigured()) {
            return Optional.of(valley.choose(sample, x, y, z));
        }
        if (sample.climate().isAvailable()
                && sample.climate().temperature() < 0.20D
                && sample.surfaceHeight() > context.seaLevel() + 4) {
            return Optional.of(Blocks.SNOW_BLOCK.getDefaultState());
        }
        if (landSurface.isConfigured()) {
            return Optional.of(landSurface.choose(sample, x, y, z));
        }
        return Optional.empty();
    }

    @Override
    public BlockState fallbackSurfaceState(TerrainSample sample) {
        return fallbackSurfaceState(sample, 0, 0);
    }

    @Override
    public BlockState fallbackSurfaceState(TerrainSample sample, int x, int z) {
        int y = solidSurfaceY(sample);
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())) {
            return oceanBed.isConfigured()
                    ? oceanBed.choose(sample, x, y, z)
                    : marine.bed(sample, x, y, z, context.seaLevel());
        }
        return landSurface.choose(sample, x, y, z);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample) {
        return hydrologyBedState(sample, 0, solidSurfaceY(sample), 0);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample, int x, int y, int z) {
        if (StandardTerrainTypes.LAKE.equals(sample.terrainType()) && lakeBed.isConfigured()) {
            return lakeBed.choose(sample, x, y, z);
        }
        if (StandardTerrainTypes.RIVER.equals(sample.terrainType()) && riverBed.isConfigured()) {
            return riverBed.choose(sample, x, y, z);
        }
        return watercourses.bed(sample, x, y, z);
    }

    @Override
    public BlockState hydrologySealState(TerrainSample sample) {
        return seal.choose(sample);
    }

    @Override
    public BlockState hydrologySealState(TerrainSample sample, int x, int y, int z) {
        return seal.choose(sample, x, y, z);
    }

    @Override
    public boolean mayRepairHydrologyGap(TerrainSample sample) {
        TerrainType terrain = sample.terrainType();
        RiverSample river = sample.river();
        boolean insideDryChannel = river.isAvailable()
                && river.hasFlow()
                && river.flow() > 0.0D
                && river.distance() <= Math.max(0.5D, river.width() * 0.5D);
        return StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)
                || insideDryChannel
                || river.hasWaterSurfaceHeight() && river.depth() > MIN_WET_DEPTH;
    }

    @Override
    public int hydrologyGapBedY(TerrainSample sample, int waterTopExclusive) {
        int waterBlocks = waterTopExclusive <= context.seaLevel() + 2 ? 3 : 2;
        int lowestWetBed = waterTopExclusive - waterBlocks - 1;
        return clamp(
                Math.min(solidSurfaceY(sample), lowestWetBed),
                context.minY() + 1,
                context.maxYExclusive() - 2);
    }

    @Override
    public int hydrologyGapRepairRadius() {
        return hydrologyGapRepairRadius;
    }

    @Override
    public int hydrologyCaveMargin() {
        return hydrologyCaveMargin;
    }

    @Override
    public int hydrologyBedSealDepth() {
        return hydrologyBedSealDepth;
    }

    @Override
    public int hydrologyBankSealDepth() {
        return hydrologyBankSealDepth;
    }

    @Override
    public void decorateWatercourses(WaterDecorationContext context) {
        decorator.decorate(context);
    }

    private int integerOption(String key, int fallback, int min, int max) {
        String raw = context.options().get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(
                        "Materializer option '" + key + "' must be in [" + min + ", " + max + "]");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Materializer option '" + key + "' must be an integer", exception);
        }
    }

    private ConfiguredBlockSet set(String key, BlockState fallback) {
        return ConfiguredBlockSet.parse(context.options(), key, List.of(fallback));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
