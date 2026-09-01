package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.core.biome.BiomeClimateRouter;
import dev.foucaultleon.flterraforged.core.biome.BiomeRole;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * Built-in height- and climate-aware full-block palettes for surface watercourses.
 *
 * <p>The palette deliberately separates submerged bed, damp waterline, dry transition and bank
 * filler. It uses only stable full-block states; plants, carpets, slabs, stairs, falling powder and
 * waterfall spray require a later decoration/partial-block pass and are not silently substituted
 * into the structural terrain layer.</p>
 */
final class WatercourseMaterialPalette {

    private final Map<Profile, Palette> palettes;

    /**
     * Creates all standard palettes, applying optional profile-specific configuration overrides.
     *
     * @param options materializer configuration
     */
    WatercourseMaterialPalette(Map<String, String> options) {
        palettes = new EnumMap<>(Profile.class);
        palettes.put(Profile.HIGH_ALPINE, palette(options, Profile.HIGH_ALPINE,
                list(Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.COBBLESTONE,
                        Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.ANDESITE, Blocks.GRAVEL,
                        Blocks.GRAVEL, Blocks.CALCITE),
                list(Blocks.STONE, Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE,
                        Blocks.SNOW_BLOCK),
                list(Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.SNOW_BLOCK,
                        Blocks.SNOW_BLOCK),
                list(Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE)));
        palettes.put(Profile.SNOWY_HIGHLAND, palette(options, Profile.SNOWY_HIGHLAND,
                list(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.COBBLESTONE,
                        Blocks.MOSSY_COBBLESTONE, Blocks.MUD, Blocks.DIORITE, Blocks.PACKED_ICE),
                list(Blocks.PODZOL, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MUD,
                        Blocks.MOSSY_COBBLESTONE, Blocks.PACKED_ICE),
                list(Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT,
                        Blocks.MOSSY_COBBLESTONE, Blocks.SNOW_BLOCK),
                list(Blocks.COARSE_DIRT, Blocks.MUD, Blocks.STONE, Blocks.COBBLESTONE)));
        palettes.put(Profile.ALPINE, palette(options, Profile.ALPINE,
                list(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.COBBLESTONE,
                        Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.STONE, Blocks.CALCITE),
                list(Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.STONE, Blocks.GRAVEL,
                        Blocks.MOSSY_COBBLESTONE),
                list(Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.COARSE_DIRT),
                list(Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.COARSE_DIRT)));
        palettes.put(Profile.FOREST, palette(options, Profile.FOREST,
                list(Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, Blocks.GRAVEL, Blocks.GRAVEL,
                        Blocks.GRAVEL, Blocks.SAND, Blocks.SAND, Blocks.MUD, Blocks.MOSS_BLOCK),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK,
                        Blocks.ROOTED_DIRT, Blocks.MUD),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.ROOTED_DIRT,
                        Blocks.COARSE_DIRT, Blocks.MOSS_BLOCK),
                list(Blocks.DIRT, Blocks.DIRT, Blocks.ROOTED_DIRT, Blocks.MUD)));
        palettes.put(Profile.DARK_FOREST, palette(options, Profile.DARK_FOREST,
                list(Blocks.MUD, Blocks.MUD, Blocks.MUD, Blocks.CLAY, Blocks.CLAY,
                        Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.MOSSY_COBBLESTONE),
                list(Blocks.PODZOL, Blocks.PODZOL, Blocks.MUD, Blocks.MOSS_BLOCK,
                        Blocks.MYCELIUM, Blocks.MOSSY_COBBLESTONE),
                list(Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM, Blocks.MOSS_BLOCK),
                list(Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.MUD, Blocks.ROOTED_DIRT)));
        palettes.put(Profile.JUNGLE, palette(options, Profile.JUNGLE,
                list(Blocks.MUD, Blocks.MUD, Blocks.MUD, Blocks.CLAY, Blocks.CLAY, Blocks.SAND,
                        Blocks.SAND, Blocks.MOSS_BLOCK, Blocks.FIRE_CORAL_BLOCK),
                list(Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.MUD, Blocks.ROOTED_DIRT,
                        Blocks.GRASS_BLOCK),
                list(Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK, Blocks.ROOTED_DIRT, Blocks.MUD),
                list(Blocks.MUD, Blocks.DIRT, Blocks.ROOTED_DIRT, Blocks.CLAY)));
        palettes.put(Profile.MIDLAND, palette(options, Profile.MIDLAND,
                list(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.CLAY, Blocks.CLAY,
                        Blocks.MUD, Blocks.SAND, Blocks.COBBLESTONE),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.MUD,
                        Blocks.ROOTED_DIRT),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
                        Blocks.ROOTED_DIRT),
                list(Blocks.DIRT, Blocks.DIRT, Blocks.ROOTED_DIRT, Blocks.MUD)));
        palettes.put(Profile.PLAINS, palette(options, Profile.PLAINS,
                list(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.GRAVEL, Blocks.GRAVEL,
                        Blocks.GRAVEL, Blocks.CLAY, Blocks.CLAY, Blocks.MUD),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.SAND,
                        Blocks.ROOTED_DIRT),
                list(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.SAND),
                list(Blocks.DIRT, Blocks.DIRT, Blocks.MUD, Blocks.SAND)));
        palettes.put(Profile.DRYLAND, palette(options, Profile.DRYLAND,
                list(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.RED_SAND,
                        Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.GRAVEL, Blocks.RED_TERRACOTTA),
                list(Blocks.SAND, Blocks.SAND, Blocks.RED_SAND, Blocks.MUD, Blocks.MUD,
                        Blocks.GRASS_BLOCK, Blocks.SANDSTONE),
                list(Blocks.SAND, Blocks.SAND, Blocks.RED_SAND, Blocks.SMOOTH_SANDSTONE,
                        Blocks.RED_TERRACOTTA),
                list(Blocks.SAND, Blocks.SANDSTONE, Blocks.RED_SAND, Blocks.RED_TERRACOTTA)));
        palettes.put(Profile.WETLAND, palette(options, Profile.WETLAND,
                list(Blocks.MUD, Blocks.MUD, Blocks.MUD, Blocks.MUD, Blocks.CLAY, Blocks.CLAY,
                        Blocks.MOSSY_COBBLESTONE, Blocks.MANGROVE_ROOTS, Blocks.GRAVEL),
                list(Blocks.MUD, Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.MANGROVE_ROOTS,
                        Blocks.MOSSY_COBBLESTONE),
                list(Blocks.MUD, Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK,
                        Blocks.MANGROVE_ROOTS),
                list(Blocks.MUD, Blocks.MUD, Blocks.CLAY, Blocks.ROOTED_DIRT)));
        palettes.put(Profile.LUSH_UNDERGROUND, palette(options, Profile.LUSH_UNDERGROUND,
                list(Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, Blocks.MOSS_BLOCK, Blocks.MUD,
                        Blocks.SAND, Blocks.ROOTED_DIRT),
                list(Blocks.MOSS_BLOCK, Blocks.MOSS_BLOCK, Blocks.CLAY, Blocks.ROOTED_DIRT),
                list(Blocks.MOSS_BLOCK, Blocks.ROOTED_DIRT, Blocks.CLAY, Blocks.STONE),
                list(Blocks.CLAY, Blocks.ROOTED_DIRT, Blocks.STONE, Blocks.MOSS_BLOCK)));
        palettes.put(Profile.ROCKY_UNDERGROUND, palette(options, Profile.ROCKY_UNDERGROUND,
                list(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.DRIPSTONE_BLOCK, Blocks.DRIPSTONE_BLOCK,
                        Blocks.STONE, Blocks.GRANITE),
                list(Blocks.DRIPSTONE_BLOCK, Blocks.STONE, Blocks.GRANITE, Blocks.GRAVEL),
                list(Blocks.STONE, Blocks.GRANITE, Blocks.DRIPSTONE_BLOCK),
                list(Blocks.STONE, Blocks.STONE, Blocks.GRANITE, Blocks.DRIPSTONE_BLOCK)));
        palettes.put(Profile.DEEPSLATE, palette(options, Profile.DEEPSLATE,
                list(Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE,
                        Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.SEA_LANTERN),
                list(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE, Blocks.TUFF),
                list(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF),
                list(Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF)));
    }

    /** Returns a deterministic submerged-bed material. */
    BlockState bed(TerrainSample sample, int x, int y, int z) {
        return palette(sample, y).bed().choose(sample, x, y, z);
    }

    /** Returns a deterministic material for the damp waterline. */
    BlockState wetBank(TerrainSample sample, int x, int y, int z) {
        return palette(sample, y).wetBank().choose(sample, x, y, z);
    }

    /** Returns a deterministic material for the outer dry transition. */
    BlockState dryBank(TerrainSample sample, int x, int y, int z) {
        return palette(sample, y).dryBank().choose(sample, x, y, z);
    }

    /** Returns a stable filler below either bank transition. */
    BlockState bankFiller(TerrainSample sample, int x, int y, int z) {
        return palette(sample, y).bankFiller().choose(sample, x, y, z);
    }

    private Palette palette(TerrainSample sample, int y) {
        int elevation = sample.river().hasWaterSurfaceHeight()
                ? (int) Math.floor(sample.river().waterSurfaceHeight())
                : y;
        return palettes.get(profile(sample, elevation));
    }

    private static Profile profile(TerrainSample sample, int y) {
        BiomeRole role = BiomeClimateRouter.route(sample);
        double temperature = sample.climate().isAvailable() ? sample.climate().temperature() : 0.5D;
        double moisture = sample.climate().isAvailable() ? sample.climate().moisture() : 0.5D;
        if (y < 0) {
            return Profile.DEEPSLATE;
        }
        if (y < 60) {
            return moisture > 0.64D && (!sample.hasSlope() || sample.slope() < 0.75D)
                    ? Profile.LUSH_UNDERGROUND
                    : Profile.ROCKY_UNDERGROUND;
        }
        if (y > 120) {
            return Profile.HIGH_ALPINE;
        }
        if (y >= 90) {
            return temperature < 0.30D || role == BiomeRole.BOREAL_FOREST
                    ? Profile.SNOWY_HIGHLAND
                    : Profile.ALPINE;
        }
        if (y <= 63) {
            if ((temperature > 0.68D && moisture < 0.42D)
                    || role == BiomeRole.HOT_DRY
                    || role == BiomeRole.HOT_SEASONAL
                    || role == BiomeRole.MEDITERRANEAN_GRASSLAND) {
                return Profile.DRYLAND;
            }
            return role == BiomeRole.WETLAND || moisture > 0.78D
                    ? Profile.WETLAND
                    : Profile.PLAINS;
        }
        if (temperature > 0.82D && moisture > 0.58D) {
            return Profile.JUNGLE;
        }
        if (moisture > 0.78D && (!sample.hasSlope() || sample.slope() < 0.45D)) {
            return Profile.WETLAND;
        }
        if (temperature > 0.72D && moisture < 0.36D) {
            return Profile.DRYLAND;
        }
        if (moisture > 0.72D) {
            return Profile.DARK_FOREST;
        }
        if (moisture > 0.54D) {
            return Profile.FOREST;
        }
        return switch (role) {
            case HOT_WET -> Profile.JUNGLE;
            case TEMPERATE_DENSE_FOREST -> Profile.DARK_FOREST;
            case BOREAL_FOREST, COOL_FOREST, TEMPERATE_FOREST,
                    TEMPERATE_OPEN_WOODLAND, MEDITERRANEAN_WOODLAND -> Profile.FOREST;
            case WETLAND -> Profile.WETLAND;
            case HOT_DRY, HOT_SEASONAL -> Profile.DRYLAND;
            default -> Profile.MIDLAND;
        };
    }

    private static Palette palette(
            Map<String, String> options,
            Profile profile,
            List<BlockState> bed,
            List<BlockState> wetBank,
            List<BlockState> dryBank,
            List<BlockState> bankFiller) {
        String prefix = "blockset.watercourse." + profile.key;
        return new Palette(
                ConfiguredBlockSet.parse(options, prefix + ".bed", bed),
                ConfiguredBlockSet.parse(options, prefix + ".wet_bank", wetBank),
                ConfiguredBlockSet.parse(options, prefix + ".dry_bank", dryBank),
                ConfiguredBlockSet.parse(options, prefix + ".bank_filler", bankFiller));
    }

    private static List<BlockState> list(net.minecraft.block.Block... blocks) {
        return java.util.Arrays.stream(blocks).map(block -> block.getDefaultState()).toList();
    }

    private enum Profile {
        HIGH_ALPINE("high_alpine"),
        SNOWY_HIGHLAND("snowy_highland"),
        ALPINE("alpine"),
        FOREST("forest"),
        DARK_FOREST("dark_forest"),
        JUNGLE("jungle"),
        MIDLAND("midland"),
        PLAINS("plains"),
        DRYLAND("dryland"),
        WETLAND("wetland"),
        LUSH_UNDERGROUND("lush_underground"),
        ROCKY_UNDERGROUND("rocky_underground"),
        DEEPSLATE("deepslate");

        private final String key;

        Profile(String key) {
            this.key = key;
        }
    }

    private record Palette(
            ConfiguredBlockSet bed,
            ConfiguredBlockSet wetBank,
            ConfiguredBlockSet dryBank,
            ConfiguredBlockSet bankFiller) {
    }
}
