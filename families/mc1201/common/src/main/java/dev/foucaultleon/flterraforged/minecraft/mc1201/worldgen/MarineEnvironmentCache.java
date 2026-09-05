package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerHeightQuantizer;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainEnvironmentSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import java.util.Objects;

/**
 * Bounded multi-stage cache for marine structure and open-water environment checks.
 *
 * <p>Level one stores provider-resolved materialized columns. Level two stores a compact near/far
 * water-body profile that distinguishes a confined channel from broad marine water. Level three
 * retains the larger structure rings requested by the existing shipwreck/ruin/monument rules. Every
 * stage reuses the same canonical column entries, so a sample requested by more than one rule is not
 * regenerated.</p>
 *
 * <p>Cold keys use synchronous single-flight ownership. One world-generation thread computes each
 * missing column, profile or ring directly and concurrent callers for the same key reuse its
 * completed result. Loaders do not submit executor work and do not execute while a completed-cache
 * monitor is held. The dependency graph remains acyclic: {@code ring/profile -> column ->
 * TerrainWorld.environment}.</p>
 *
 * <p>All wet/open-water decisions use provider-aware physical geometry. An overlapping water plane
 * does not turn a non-waterloggable partial-height top cell into usable marine water merely because
 * both occupy the same integer Minecraft cell. Conquest-style variable-height providers therefore
 * participate through the generic materializer SPI without hardcoded block identifiers.</p>
 */
final class MarineEnvironmentCache {

    private static final int COLUMN_CACHE_SIZE = 8192;
    private static final int PROFILE_CACHE_SIZE = 4096;
    private static final int RING_CACHE_SIZE = 4096;

    private static final int NEAR_RADIUS = 8;
    private static final int FAR_RADIUS = 16;
    private static final int INNER_RADIUS = 32;
    private static final int OUTER_RADIUS = 64;
    private static final int INNER_RING = 0;
    private static final int OUTER_RING = 1;

    private static final int[][] NEAR_OFFSETS = {
        {0, -NEAR_RADIUS},
        {-NEAR_RADIUS, 0},
        {NEAR_RADIUS, 0},
        {0, NEAR_RADIUS},
        {-NEAR_RADIUS, -NEAR_RADIUS},
        {NEAR_RADIUS, -NEAR_RADIUS},
        {-NEAR_RADIUS, NEAR_RADIUS},
        {NEAR_RADIUS, NEAR_RADIUS}
    };
    private static final int[][] FAR_OFFSETS = {
        {0, -FAR_RADIUS},
        {-FAR_RADIUS, 0},
        {FAR_RADIUS, 0},
        {0, FAR_RADIUS}
    };
    private static final int[][] INNER_OFFSETS = {
        {0, -INNER_RADIUS},
        {-INNER_RADIUS, 0},
        {INNER_RADIUS, 0},
        {0, INNER_RADIUS}
    };
    private static final int[][] OUTER_OFFSETS = {
        {0, -OUTER_RADIUS},
        {-OUTER_RADIUS, 0},
        {OUTER_RADIUS, 0},
        {0, OUTER_RADIUS}
    };

    private final BlockMaterializer materializer;
    private final WorldgenSingleFlightCache<ColumnKey, MarineColumn> columns =
            new WorldgenSingleFlightCache<>(COLUMN_CACHE_SIZE);
    private final WorldgenSingleFlightCache<ProfileKey, OpenWaterProfile> profiles =
            new WorldgenSingleFlightCache<>(PROFILE_CACHE_SIZE);
    private final WorldgenSingleFlightCache<RingKey, RingStats> rings =
            new WorldgenSingleFlightCache<>(RING_CACHE_SIZE);

    MarineEnvironmentCache(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    MarineColumn column(TerrainWorld world, int x, int z) {
        Objects.requireNonNull(world, "world");
        ColumnKey key = new ColumnKey(world, x, z);
        return columns.get(key, () -> resolveColumn(world, x, z));
    }

    OpenWaterProfile waterBodyProfile(TerrainWorld world, int centerX, int centerZ) {
        Objects.requireNonNull(world, "world");
        ProfileKey key = new ProfileKey(world, centerX, centerZ);
        return profiles.get(key, () -> resolveProfile(world, centerX, centerZ));
    }

    RingStats innerRing(TerrainWorld world, int centerX, int centerZ) {
        return ring(world, centerX, centerZ, INNER_RING, INNER_OFFSETS);
    }

    RingStats outerRing(TerrainWorld world, int centerX, int centerZ) {
        return ring(world, centerX, centerZ, OUTER_RING, OUTER_OFFSETS);
    }

    int cachedColumns() {
        return columns.completedSize();
    }

    int cachedProfiles() {
        return profiles.completedSize();
    }

    int cachedSummaries() {
        return rings.completedSize();
    }

    private RingStats ring(
            TerrainWorld world,
            int centerX,
            int centerZ,
            int ringId,
            int[][] offsets) {
        Objects.requireNonNull(world, "world");
        RingKey key = new RingKey(world, centerX, centerZ, ringId);
        return rings.get(key, () -> sampleRing(world, centerX, centerZ, offsets));
    }

    private MarineColumn resolveColumn(TerrainWorld world, int x, int z) {
        TerrainEnvironmentSample sample = world.environment(x, z);
        TerrainType terrainType = sample.terrainType();
        boolean ocean = StandardTerrainTypes.OCEAN.equals(terrainType);
        boolean coast = StandardTerrainTypes.COAST.equals(terrainType);
        boolean inlandWater = StandardTerrainTypes.RIVER.equals(terrainType)
                || StandardTerrainTypes.LAKE.equals(terrainType)
                || StandardTerrainTypes.LAKE_SHORE.equals(terrainType);
        MaterializedSurfaceGeometry geometry =
                MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
        int waterTop = materializedWaterTop(sample);
        boolean materializedWater = waterTop != Integer.MIN_VALUE
                && MaterializerGeometry.hasMaterializableWater(
                        materializer,
                        sample,
                        geometry,
                        x,
                        z,
                        waterTop);
        double waterDepth = materializedWater
                ? Math.max(0.0D, waterTop - geometry.topY())
                : 0.0D;
        return new MarineColumn(
                sample,
                geometry,
                ocean,
                coast,
                inlandWater,
                materializedWater,
                waterDepth);
    }

    private OpenWaterProfile resolveProfile(
            TerrainWorld world,
            int centerX,
            int centerZ) {
        MarineColumn center = column(world, centerX, centerZ);
        TerrainType type = center.sample().terrainType();

        if (center.materializedWater() && center.inlandWater()) {
            WaterBodyKind kind = StandardTerrainTypes.RIVER.equals(type)
                    ? WaterBodyKind.RIVER
                    : WaterBodyKind.LAKE;
            return OpenWaterProfile.inland(kind);
        }

        // A dry non-coast center cannot be a marine start and does not need neighborhood work.
        // Dry coast centers remain eligible for the stencil because beached shipwrecks need proof
        // that the beach actually fronts open sea rather than a bounded wet depression.
        if (!center.isMarineWater() && !center.coast()) {
            return OpenWaterProfile.dry();
        }

        int nearMarine = 0;
        int nearOcean = 0;
        int nearDry = 0;
        int nearInland = 0;
        boolean[] nearMarineFlags = new boolean[NEAR_OFFSETS.length];
        for (int index = 0; index < NEAR_OFFSETS.length; index++) {
            int[] offset = NEAR_OFFSETS[index];
            MarineColumn sampled = column(world, centerX + offset[0], centerZ + offset[1]);
            if (sampled.inlandWater()) {
                nearInland++;
            }
            if (sampled.isMarineWater()) {
                nearMarine++;
                nearMarineFlags[index] = true;
                if (sampled.ocean()) {
                    nearOcean++;
                }
            } else if (!sampled.materializedWater()) {
                nearDry++;
            }
        }

        int farMarine = 0;
        int farOcean = 0;
        int farInland = 0;
        for (int[] offset : FAR_OFFSETS) {
            MarineColumn sampled = column(world, centerX + offset[0], centerZ + offset[1]);
            if (sampled.inlandWater()) {
                farInland++;
            }
            if (sampled.isMarineWater()) {
                farMarine++;
                if (sampled.ocean()) {
                    farOcean++;
                }
            }
        }

        boolean broadX = nearMarineFlags[1] && nearMarineFlags[2];
        boolean broadZ = nearMarineFlags[0] && nearMarineFlags[3];
        boolean openMarine = center.isMarineWater()
                && nearInland == 0
                && farInland == 0
                && nearMarine >= 5
                && farOcean >= 2
                && (broadX || broadZ);
        boolean openMarineAccess = nearInland == 0
                && farInland == 0
                && nearMarine >= 2
                && farOcean >= 1;

        WaterBodyKind kind;
        if (!center.materializedWater()) {
            kind = WaterBodyKind.DRY;
        } else if (openMarine) {
            kind = WaterBodyKind.OPEN_MARINE;
        } else {
            kind = WaterBodyKind.CONFINED_CHANNEL;
        }
        return new OpenWaterProfile(
                kind,
                nearMarine,
                nearOcean,
                nearDry,
                nearInland,
                farMarine,
                farOcean,
                farInland,
                broadX,
                broadZ,
                openMarineAccess);
    }

    private int materializedWaterTop(TerrainEnvironmentSample sample) {
        if (!sample.hasWaterSurfaceHeight()) {
            return Integer.MIN_VALUE;
        }
        int waterTop = MaterializerHeightQuantizer.exclusiveFluidTop(
                sample.waterSurfaceHeight());
        waterTop = Math.max(materializer.context().minY(), waterTop);
        return Math.min(materializer.context().maxYExclusive(), waterTop);
    }

    private RingStats sampleRing(
            TerrainWorld world,
            int centerX,
            int centerZ,
            int[][] offsets) {
        int marineWater = 0;
        int oceanWater = 0;
        int coast = 0;
        int inlandWater = 0;
        double minimumMarineDepth = Double.POSITIVE_INFINITY;
        for (int[] offset : offsets) {
            MarineColumn column = column(world, centerX + offset[0], centerZ + offset[1]);
            if (column.inlandWater()) {
                inlandWater++;
            }
            if (column.coast()) {
                coast++;
            }
            if (column.isMarineWater()) {
                marineWater++;
                minimumMarineDepth = Math.min(minimumMarineDepth, column.waterDepth());
                if (column.ocean()) {
                    oceanWater++;
                }
            }
        }
        if (marineWater == 0) {
            minimumMarineDepth = 0.0D;
        }
        return new RingStats(
                offsets.length,
                marineWater,
                oceanWater,
                coast,
                inlandWater,
                minimumMarineDepth);
    }

    /** Materialized semantic information for one sampled X/Z column. */
    record MarineColumn(
            TerrainEnvironmentSample sample,
            MaterializedSurfaceGeometry geometry,
            boolean ocean,
            boolean coast,
            boolean inlandWater,
            boolean materializedWater,
            double waterDepth) {

        boolean isMarineWater() {
            return materializedWater && (ocean || coast);
        }
    }

    /**
     * Compact provider-resolved water-body profile around one candidate center.
     *
     * @param kind resolved center water-body kind
     * @param nearMarine material marine samples in the eight-point near stencil
     * @param nearOcean ocean-semantic samples in the near stencil
     * @param nearDry dry samples in the near stencil
     * @param nearInland inland-water samples in the near stencil
     * @param farMarine material marine samples in the four-point far stencil
     * @param farOcean ocean-semantic samples in the far stencil
     * @param farInland inland-water samples in the far stencil
     * @param broadX whether marine water spans both near X probes
     * @param broadZ whether marine water spans both near Z probes
     * @param openMarineAccess whether the center/neighborhood has a minimal clean route to open sea
     */
    record OpenWaterProfile(
            WaterBodyKind kind,
            int nearMarine,
            int nearOcean,
            int nearDry,
            int nearInland,
            int farMarine,
            int farOcean,
            int farInland,
            boolean broadX,
            boolean broadZ,
            boolean openMarineAccess) {

        static OpenWaterProfile dry() {
            return new OpenWaterProfile(
                    WaterBodyKind.DRY, 0, 0, 0, 0, 0, 0, 0, false, false, false);
        }

        static OpenWaterProfile inland(WaterBodyKind kind) {
            if (kind != WaterBodyKind.RIVER && kind != WaterBodyKind.LAKE) {
                throw new IllegalArgumentException("inland profile requires RIVER or LAKE");
            }
            return new OpenWaterProfile(kind, 0, 0, 0, 1, 0, 0, 0, false, false, false);
        }

        boolean isOpenMarine() {
            return kind == WaterBodyKind.OPEN_MARINE;
        }
    }

    /** Aggregated ring statistics without retaining individual sampled columns. */
    record RingStats(
            int samples,
            int marineWater,
            int oceanWater,
            int coast,
            int inlandWater,
            double minimumMarineDepth) {

        double marineFraction() {
            return samples == 0 ? 0.0D : (double) marineWater / samples;
        }

        double oceanFraction() {
            return samples == 0 ? 0.0D : (double) oceanWater / samples;
        }
    }

    private record ColumnKey(TerrainWorld world, int x, int z) {
        ColumnKey {
            Objects.requireNonNull(world, "world");
        }
    }

    private record ProfileKey(TerrainWorld world, int x, int z) {
        ProfileKey {
            Objects.requireNonNull(world, "world");
        }
    }

    private record RingKey(TerrainWorld world, int x, int z, int ringId) {
        RingKey {
            Objects.requireNonNull(world, "world");
            if (ringId != INNER_RING && ringId != OUTER_RING) {
                throw new IllegalArgumentException("unknown marine ring: " + ringId);
            }
        }
    }
}
