package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializedSurfaceGeometry;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerGeometry;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;

/**
 * FlTerraForged-owned deterministic cave and ravine carver for Minecraft 1.20.1.
 *
 * <p>R49 keeps the depth-aware ocean/lake floor seal introduced by R48, but the protection decision
 * no longer depends solely on already-quantized Minecraft water. A column that is semantically wet
 * in Engine space is protected before block quantization, so a near-integer water height cannot
 * simultaneously lose its final fluid cell and its carver roof.</p>
 *
 * <p>The physical surface geometry, water envelope and carve ceiling are resolved once for the
 * 18-by-18 target tile and then reused by every cave/ravine source. Variable-height providers retain
 * control of physical geometry and wet states through the materializer API; the semantic wet flag is
 * only an additional destructive-carving guard.</p>
 */
final class FlTerraForgedCarver {

    private static final int CHUNK_SIZE = 16;
    private static final int HALO = 1;
    private static final int WIDTH = CHUNK_SIZE + HALO * 2;
    private static final int AREA = WIDTH * WIDTH;
    private static final int SOURCE_RADIUS = 2;
    private static final int BEDROCK_MARGIN = 5;
    private static final double CAVE_ORIGIN_CHANCE = 0.18D;
    private static final double RAVINE_ORIGIN_CHANCE = 0.028D;
    private static final double GEOMETRY_EPSILON = 1.0E-6D;
    private static final double SEMANTIC_WET_DEPTH = 0.05D;
    private static final int OCEAN_FLOOR_ROOF = 7;
    private static final int LAKE_FLOOR_ROOF = 6;
    private static final int RIVER_FLOOR_ROOF = 3;
    private static final int WATER_PRIORITY_RIVER = 1;
    private static final int WATER_PRIORITY_LAKE = 2;
    private static final int WATER_PRIORITY_OCEAN = 3;
    private static final long X_SALT = 0x9E3779B97F4A7C15L;
    private static final long Z_SALT = 0xC2B2AE3D27D4EB4FL;
    private static final long CAVE_SALT = 0xA24BAED4963EE407L;
    private static final long RAVINE_SALT = 0x9FB21C651E98DF25L;

    private final BlockMaterializer materializer;
    private final MaterializerContext context;

    /**
     * Creates the owned carver.
     *
     * @param materializer active block materializer
     */
    FlTerraForgedCarver(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
    }

    /**
     * Applies the requested carving step.
     *
     * <p>R49 owns AIR carving. LIQUID remains a no-op because water is selected while the immutable
     * AIR mask is materialized; a second destructive liquid stage would reintroduce ordering bugs.</p>
     *
     * @param seed world seed supplied by Minecraft's carving stage
     * @param chunk target chunk
     * @param step requested vanilla carving phase
     * @param world bound Engine terrain world
     */
    void carve(long seed, Chunk chunk, GenerationStep.Carver step, TerrainWorld world) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(step, "step");
        Objects.requireNonNull(world, "world");
        if (step != GenerationStep.Carver.AIR) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        int originX = chunkPos.getStartX() - HALO;
        int originZ = chunkPos.getStartZ() - HALO;
        TerrainSample[] samples = world.sampleTile(originX, originZ, WIDTH);
        if (samples.length != AREA) {
            throw new IllegalStateException("TerrainWorld returned an invalid carver sample tile");
        }

        SurfaceEnvelope surface = buildSurfaceEnvelope(originX, originZ, samples);
        int worldHeight = context.maxYExclusive() - context.minY();
        boolean[] mask = new boolean[AREA * worldHeight];
        buildMask(seed, chunkPos, originX, originZ, surface.carveCeiling(), mask);

        FloodResult flood = resolveConnectedWater(surface, mask);
        materialize(chunk, originX, originZ, samples, mask, flood.level());
    }

    private SurfaceEnvelope buildSurfaceEnvelope(
            int originX,
            int originZ,
            TerrainSample[] samples) {
        int[] carveCeiling = new int[AREA];
        int[] hydraulicCeiling = new int[AREA];
        int[] wetTop = new int[AREA];
        int[] firstWaterY = new int[AREA];
        int[] wetPriority = new int[AREA];
        int[] surfaceBlockY = new int[AREA];
        boolean[] protectedSurfaceWater = new boolean[AREA];

        Arrays.fill(hydraulicCeiling, context.maxYExclusive());
        Arrays.fill(wetTop, Integer.MIN_VALUE);
        Arrays.fill(firstWaterY, Integer.MAX_VALUE);
        Arrays.fill(surfaceBlockY, Integer.MIN_VALUE);

        for (int localZ = 0; localZ < WIDTH; localZ++) {
            int z = originZ + localZ;
            for (int localX = 0; localX < WIDTH; localX++) {
                int x = originX + localX;
                int column = columnIndex(localX, localZ);
                TerrainSample sample = samples[column];
                MaterializedSurfaceGeometry geometry =
                        MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
                surfaceBlockY[column] = geometry.blockY();

                boolean materialWet = materializer.hasFinalWetEnvelope(sample, x, z);
                boolean semanticWet = expectsSurfaceWater(sample);
                boolean wet = materialWet || semanticWet;
                int waterTop = wet
                        ? clamp(materializer.waterTopExclusive(sample),
                                context.minY(), context.maxYExclusive())
                        : Integer.MIN_VALUE;
                int waterStart = wet
                        ? clamp((int) Math.ceil(geometry.topY() - GEOMETRY_EPSILON),
                                context.minY(), context.maxYExclusive())
                        : Integer.MAX_VALUE;
                int roof = surfaceRoofThickness(sample, wet, waterStart, waterTop);
                carveCeiling[column] = geometry.blockY() - roof;

                if (!wet) {
                    continue;
                }
                wetTop[column] = waterTop;
                firstWaterY[column] = waterStart;
                hydraulicCeiling[column] = waterTop;
                wetPriority[column] = waterPriority(sample);
                protectedSurfaceWater[column] = wetPriority[column] >= WATER_PRIORITY_LAKE;
            }
        }

        applyLateralSurfaceWaterSeal(
                carveCeiling,
                firstWaterY,
                protectedSurfaceWater);
        return new SurfaceEnvelope(
                carveCeiling,
                hydraulicCeiling,
                wetTop,
                firstWaterY,
                wetPriority,
                surfaceBlockY);
    }

    private static boolean expectsSurfaceWater(TerrainSample sample) {
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())) {
            return true;
        }
        RiverSample hydrology = sample.river();
        return hydrology.hasWaterSurfaceHeight()
                && hydrology.depth() > SEMANTIC_WET_DEPTH
                && hydrology.waterSurfaceHeight() > sample.surfaceHeight() + SEMANTIC_WET_DEPTH;
    }

    private static void applyLateralSurfaceWaterSeal(
            int[] carveCeiling,
            int[] firstWaterY,
            boolean[] protectedSurfaceWater) {
        int[] sealed = carveCeiling.clone();
        for (int localZ = 0; localZ < WIDTH; localZ++) {
            for (int localX = 0; localX < WIDTH; localX++) {
                int column = columnIndex(localX, localZ);
                int ceiling = carveCeiling[column];
                ceiling = lateralWaterLimit(
                        ceiling, localX - 1, localZ, firstWaterY, protectedSurfaceWater);
                ceiling = lateralWaterLimit(
                        ceiling, localX + 1, localZ, firstWaterY, protectedSurfaceWater);
                ceiling = lateralWaterLimit(
                        ceiling, localX, localZ - 1, firstWaterY, protectedSurfaceWater);
                ceiling = lateralWaterLimit(
                        ceiling, localX, localZ + 1, firstWaterY, protectedSurfaceWater);
                sealed[column] = ceiling;
            }
        }
        System.arraycopy(sealed, 0, carveCeiling, 0, AREA);
    }

    private static int lateralWaterLimit(
            int current,
            int localX,
            int localZ,
            int[] firstWaterY,
            boolean[] protectedSurfaceWater) {
        if (localX < 0 || localX >= WIDTH || localZ < 0 || localZ >= WIDTH) {
            return current;
        }
        int neighbor = columnIndex(localX, localZ);
        if (!protectedSurfaceWater[neighbor]) {
            return current;
        }
        return Math.min(current, firstWaterY[neighbor] - 1);
    }

    private int surfaceRoofThickness(
            TerrainSample sample,
            boolean wet,
            int waterStart,
            int waterTop) {
        if (wet) {
            int waterDepth = Math.max(0, waterTop - waterStart);
            if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                    || StandardTerrainTypes.COAST.equals(sample.terrainType())) {
                return OCEAN_FLOOR_ROOF + Math.min(3, waterDepth / 12);
            }
            if (StandardTerrainTypes.LAKE.equals(sample.terrainType())) {
                return LAKE_FLOOR_ROOF + Math.min(2, waterDepth / 8);
            }
            if (StandardTerrainTypes.RIVER.equals(sample.terrainType())) {
                return RIVER_FLOOR_ROOF + Math.min(1, waterDepth / 6);
            }
            return 4;
        }
        if (StandardTerrainTypes.COAST.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE_SHORE.equals(sample.terrainType())
                || StandardTerrainTypes.RIVER.equals(sample.terrainType())
                || StandardTerrainTypes.LAKE.equals(sample.terrainType())) {
            return 6;
        }
        if (StandardTerrainTypes.MOUNTAINS.equals(sample.terrainType())
                || sample.slope() > 0.85D) {
            return 2;
        }
        return 4;
    }

    private void buildMask(
            long seed,
            ChunkPos target,
            int originX,
            int originZ,
            int[] carveCeiling,
            boolean[] mask) {
        for (int sourceZ = target.z - SOURCE_RADIUS; sourceZ <= target.z + SOURCE_RADIUS; sourceZ++) {
            for (int sourceX = target.x - SOURCE_RADIUS; sourceX <= target.x + SOURCE_RADIUS; sourceX++) {
                long sourceSeed = mix(seed
                        ^ (long) sourceX * X_SALT
                        ^ (long) sourceZ * Z_SALT);
                SplittableRandom caves = new SplittableRandom(sourceSeed ^ CAVE_SALT);
                if (caves.nextDouble() < CAVE_ORIGIN_CHANCE) {
                    int count = 1 + caves.nextInt(3);
                    for (int i = 0; i < count; i++) {
                        carveCavePath(caves, sourceX, sourceZ, originX, originZ, carveCeiling, mask);
                    }
                }

                SplittableRandom ravines = new SplittableRandom(sourceSeed ^ RAVINE_SALT);
                if (ravines.nextDouble() < RAVINE_ORIGIN_CHANCE) {
                    carveRavinePath(ravines, sourceX, sourceZ, originX, originZ, carveCeiling, mask);
                }
            }
        }
    }

    private void carveCavePath(
            SplittableRandom random,
            int sourceChunkX,
            int sourceChunkZ,
            int originX,
            int originZ,
            int[] carveCeiling,
            boolean[] mask) {
        double x = sourceChunkX * 16.0D + random.nextDouble(16.0D);
        double z = sourceChunkZ * 16.0D + random.nextDouble(16.0D);
        double y = randomCarveY(random, 14, 82);
        double yaw = random.nextDouble(Math.PI * 2.0D);
        double pitch = (random.nextDouble() - 0.5D) * 0.22D;
        int length = 26 + random.nextInt(38);
        double baseRadius = 1.55D + random.nextDouble() * 2.25D;

        for (int step = 0; step < length; step++) {
            double progress = (step + 0.5D) / length;
            double profile = 0.62D + Math.sin(progress * Math.PI) * 0.58D;
            double horizontal = baseRadius * profile;
            double vertical = horizontal * (0.70D + random.nextDouble() * 0.18D);
            markEllipsoid(x, y, z, horizontal, vertical, originX, originZ, carveCeiling, mask);

            double horizontalMotion = Math.cos(pitch);
            x += Math.cos(yaw) * horizontalMotion;
            z += Math.sin(yaw) * horizontalMotion;
            y += Math.sin(pitch);
            yaw += (random.nextDouble() - 0.5D) * 0.20D;
            pitch = pitch * 0.82D + (random.nextDouble() - 0.5D) * 0.075D;
        }
    }

    private void carveRavinePath(
            SplittableRandom random,
            int sourceChunkX,
            int sourceChunkZ,
            int originX,
            int originZ,
            int[] carveCeiling,
            boolean[] mask) {
        double x = sourceChunkX * 16.0D + random.nextDouble(16.0D);
        double z = sourceChunkZ * 16.0D + random.nextDouble(16.0D);
        double y = randomCarveY(random, 18, 66);
        double yaw = random.nextDouble(Math.PI * 2.0D);
        double pitch = (random.nextDouble() - 0.5D) * 0.08D;
        int length = 44 + random.nextInt(36);
        double baseWidth = 2.2D + random.nextDouble() * 2.0D;

        for (int step = 0; step < length; step++) {
            double progress = (step + 0.5D) / length;
            double profile = 0.55D + Math.sin(progress * Math.PI) * 0.75D;
            double horizontal = baseWidth * profile;
            double vertical = horizontal * (1.28D + random.nextDouble() * 0.32D);
            markEllipsoid(x, y, z, horizontal, vertical, originX, originZ, carveCeiling, mask);

            x += Math.cos(yaw);
            z += Math.sin(yaw);
            y += Math.sin(pitch) * 0.65D;
            yaw += (random.nextDouble() - 0.5D) * 0.075D;
            pitch = pitch * 0.92D + (random.nextDouble() - 0.5D) * 0.025D;
        }
    }

    private double randomCarveY(SplittableRandom random, int floorOffset, int seaOffset) {
        int low = Math.min(context.maxYExclusive() - 8, context.minY() + floorOffset);
        int high = Math.min(context.maxYExclusive() - 8, context.seaLevel() + seaOffset);
        if (high <= low) {
            return low;
        }
        return low + random.nextDouble(high - low);
    }

    private void markEllipsoid(
            double centerX,
            double centerY,
            double centerZ,
            double radiusXz,
            double radiusY,
            int originX,
            int originZ,
            int[] carveCeiling,
            boolean[] mask) {
        int minX = Math.max(originX, (int) Math.floor(centerX - radiusXz));
        int maxX = Math.min(originX + WIDTH - 1, (int) Math.ceil(centerX + radiusXz));
        int minZ = Math.max(originZ, (int) Math.floor(centerZ - radiusXz));
        int maxZ = Math.min(originZ + WIDTH - 1, (int) Math.ceil(centerZ + radiusXz));
        int minY = Math.max(context.minY() + BEDROCK_MARGIN, (int) Math.floor(centerY - radiusY));
        int maxY = Math.min(context.maxYExclusive() - 2, (int) Math.ceil(centerY + radiusY));
        if (minX > maxX || minZ > maxZ || minY > maxY) {
            return;
        }

        double inverseXz = 1.0D / Math.max(0.25D, radiusXz);
        double inverseY = 1.0D / Math.max(0.25D, radiusY);
        for (int z = minZ; z <= maxZ; z++) {
            int localZ = z - originZ;
            double dz = (z + 0.5D - centerZ) * inverseXz;
            double dz2 = dz * dz;
            for (int x = minX; x <= maxX; x++) {
                int localX = x - originX;
                double dx = (x + 0.5D - centerX) * inverseXz;
                double horizontal = dx * dx + dz2;
                if (horizontal >= 1.0D) {
                    continue;
                }
                int column = columnIndex(localX, localZ);
                int columnMaxY = Math.min(maxY, carveCeiling[column]);
                if (columnMaxY < minY) {
                    continue;
                }
                for (int y = minY; y <= columnMaxY; y++) {
                    double dy = (y + 0.5D - centerY) * inverseY;
                    if (horizontal + dy * dy < 1.0D) {
                        mask[pack(column, y)] = true;
                    }
                }
            }
        }
    }

    private FloodResult resolveConnectedWater(
            SurfaceEnvelope surface,
            boolean[] mask) {
        int[] floodLevel = new int[mask.length];
        int[] floodPriority = new int[mask.length];
        Arrays.fill(floodLevel, Integer.MIN_VALUE);
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int column = 0; column < AREA; column++) {
            int surfaceY = surface.surfaceBlockY()[column];
            int waterTop = surface.wetTop()[column];
            if (surface.wetPriority()[column] == 0
                    || surfaceY < context.minY()
                    || surfaceY >= context.maxYExclusive()
                    || waterTop <= surfaceY) {
                continue;
            }
            int packed = pack(column, surfaceY);
            if (mask[packed]) {
                offerFlood(
                        queue,
                        floodLevel,
                        floodPriority,
                        packed,
                        surface.wetPriority()[column],
                        waterTop);
            }
        }

        seedSideWaterContacts(
                mask,
                surface.wetTop(),
                surface.firstWaterY(),
                surface.wetPriority(),
                floodLevel,
                floodPriority,
                queue);

        while (!queue.isEmpty()) {
            int packed = queue.removeFirst();
            int level = floodLevel[packed];
            int priority = floodPriority[packed];
            int vertical = packed / AREA;
            int column = packed - vertical * AREA;
            int localX = column % WIDTH;
            int localZ = column / WIDTH;
            int y = context.minY() + vertical;

            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX - 1, localZ, y, priority, level);
            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX + 1, localZ, y, priority, level);
            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX, localZ - 1, y, priority, level);
            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX, localZ + 1, y, priority, level);
            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX, localZ, y - 1, priority, level);
            offerNeighbor(queue, floodLevel, floodPriority, surface.hydraulicCeiling(), mask,
                    localX, localZ, y + 1, priority, level);
        }
        return new FloodResult(floodLevel, floodPriority);
    }

    private void seedSideWaterContacts(
            boolean[] mask,
            int[] wetTop,
            int[] firstWaterY,
            int[] wetPriority,
            int[] floodLevel,
            int[] floodPriority,
            ArrayDeque<Integer> queue) {
        for (int localZ = 0; localZ < WIDTH; localZ++) {
            for (int localX = 0; localX < WIDTH; localX++) {
                int column = columnIndex(localX, localZ);
                for (int y = context.minY() + BEDROCK_MARGIN;
                        y < context.maxYExclusive();
                        y++) {
                    int packed = pack(column, y);
                    if (!mask[packed]) {
                        continue;
                    }
                    seedFromWetNeighbor(queue, floodLevel, floodPriority, wetTop, firstWaterY,
                            wetPriority, localX - 1, localZ, packed, y);
                    seedFromWetNeighbor(queue, floodLevel, floodPriority, wetTop, firstWaterY,
                            wetPriority, localX + 1, localZ, packed, y);
                    seedFromWetNeighbor(queue, floodLevel, floodPriority, wetTop, firstWaterY,
                            wetPriority, localX, localZ - 1, packed, y);
                    seedFromWetNeighbor(queue, floodLevel, floodPriority, wetTop, firstWaterY,
                            wetPriority, localX, localZ + 1, packed, y);
                }
            }
        }
    }

    private static void seedFromWetNeighbor(
            ArrayDeque<Integer> queue,
            int[] floodLevel,
            int[] floodPriority,
            int[] wetTop,
            int[] firstWaterY,
            int[] wetPriority,
            int localX,
            int localZ,
            int packed,
            int y) {
        if (localX < 0 || localX >= WIDTH || localZ < 0 || localZ >= WIDTH) {
            return;
        }
        int neighbor = columnIndex(localX, localZ);
        if (wetPriority[neighbor] == 0
                || y < firstWaterY[neighbor]
                || y >= wetTop[neighbor]) {
            return;
        }
        offerFlood(
                queue,
                floodLevel,
                floodPriority,
                packed,
                wetPriority[neighbor],
                wetTop[neighbor]);
    }

    private void offerNeighbor(
            ArrayDeque<Integer> queue,
            int[] floodLevel,
            int[] floodPriority,
            int[] hydraulicCeiling,
            boolean[] mask,
            int localX,
            int localZ,
            int y,
            int priority,
            int level) {
        if (localX < 0
                || localX >= WIDTH
                || localZ < 0
                || localZ >= WIDTH
                || y < context.minY()
                || y >= context.maxYExclusive()) {
            return;
        }
        int column = columnIndex(localX, localZ);
        int cappedLevel = Math.min(level, hydraulicCeiling[column]);
        if (y >= cappedLevel) {
            return;
        }
        int packed = pack(column, y);
        if (!mask[packed]) {
            return;
        }
        offerFlood(queue, floodLevel, floodPriority, packed, priority, cappedLevel);
    }

    private static void offerFlood(
            ArrayDeque<Integer> queue,
            int[] floodLevel,
            int[] floodPriority,
            int packed,
            int priority,
            int level) {
        int existingPriority = floodPriority[packed];
        int existingLevel = floodLevel[packed];
        boolean better;
        if (priority > existingPriority) {
            better = true;
        } else if (priority < existingPriority) {
            better = false;
        } else if (priority >= WATER_PRIORITY_LAKE) {
            better = existingLevel == Integer.MIN_VALUE || level < existingLevel;
        } else {
            better = level > existingLevel;
        }
        if (!better) {
            return;
        }
        floodPriority[packed] = priority;
        floodLevel[packed] = level;
        queue.addLast(packed);
    }

    private static int waterPriority(TerrainSample sample) {
        if (StandardTerrainTypes.OCEAN.equals(sample.terrainType())
                || StandardTerrainTypes.COAST.equals(sample.terrainType())) {
            return WATER_PRIORITY_OCEAN;
        }
        if (StandardTerrainTypes.LAKE.equals(sample.terrainType())) {
            return WATER_PRIORITY_LAKE;
        }
        return WATER_PRIORITY_RIVER;
    }

    private void materialize(
            Chunk chunk,
            int originX,
            int originZ,
            TerrainSample[] samples,
            boolean[] mask,
            int[] floodLevel) {
        ChunkPos pos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            int haloZ = localZ + HALO;
            int z = pos.getStartZ() + localZ;
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int haloX = localX + HALO;
                int x = pos.getStartX() + localX;
                int column = columnIndex(haloX, haloZ);
                TerrainSample sample = samples[column];
                MaterializedSurfaceGeometry geometry =
                        MaterializerGeometry.surfaceGeometry(materializer, sample, x, z);
                int surfaceY = Math.min(context.maxYExclusive() - 2, geometry.blockY());
                for (int y = context.minY() + BEDROCK_MARGIN; y <= surfaceY; y++) {
                    int packed = pack(column, y);
                    if (!mask[packed]) {
                        continue;
                    }
                    mutable.set(x, y, z);
                    BlockState current = chunk.getBlockState(mutable);
                    if (current.isOf(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState target;
                    if (floodLevel[packed] > y) {
                        if (materializer.permitsFinalWetFlow(sample, current, x, y, z)) {
                            target = materializer.finalWetState(sample, current, x, y, z);
                        } else {
                            target = materializer.fluidState(sample);
                        }
                    } else if (!current.getFluidState().isEmpty()) {
                        target = current;
                    } else {
                        target = materializer.airState(sample);
                    }
                    if (!target.equals(current)) {
                        chunk.setBlockState(mutable, target, false);
                    }
                }
            }
        }
    }

    private int pack(int column, int y) {
        return (y - context.minY()) * AREA + column;
    }

    private static int columnIndex(int localX, int localZ) {
        return localZ * WIDTH + localX;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private record SurfaceEnvelope(
            int[] carveCeiling,
            int[] hydraulicCeiling,
            int[] wetTop,
            int[] firstWaterY,
            int[] wetPriority,
            int[] surfaceBlockY) {
    }

    private record FloodResult(int[] level, int[] priority) {
    }
}
