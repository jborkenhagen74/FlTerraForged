package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
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
 * <p>The carver first constructs an immutable 3D carve mask for the target chunk plus a one-block
 * horizontal halo. No Minecraft block is modified while cave geometry is being decided. A second
 * phase floods only mask components that actually breach an Engine/materializer wet surface. Wet
 * river/lake/ocean columns also impose their own hydraulic ceiling, so a high upstream river cannot
 * back-fill a lower downstream reach through a connected cave and erase a legitimate waterfall.
 * The final phase writes the target chunk once. This avoids the former sequence of vanilla carving
 * followed by one or more hydrology reconstruction passes.</p>
 *
 * <p>Feature origins are derived from surrounding source chunks, so a cave crossing a chunk border
 * is generated from the same world-space definition regardless of chunk generation order.</p>
 */
final class FlTerraForgedCarver {

    private static final int CHUNK_SIZE = 16;
    private static final int HALO = 1;
    private static final int WIDTH = CHUNK_SIZE + HALO * 2;
    private static final int AREA = WIDTH * WIDTH;
    private static final int SOURCE_RADIUS = 2;
    private static final int BEDROCK_MARGIN = 5;
    private static final double CAVE_ORIGIN_CHANCE = 0.24D;
    private static final double RAVINE_ORIGIN_CHANCE = 0.045D;
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
     * <p>R46 owns AIR carving. LIQUID is intentionally a no-op because water is selected directly
     * while the AIR mask is materialized; running a second destructive liquid stage would recreate
     * the ordering problem this carver replaces.</p>
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

        int worldHeight = context.maxYExclusive() - context.minY();
        boolean[] mask = new boolean[AREA * worldHeight];
        buildMask(seed, chunkPos, originX, originZ, samples, mask);

        int[] floodLevel = resolveConnectedWater(originX, originZ, samples, mask);
        materialize(chunk, originX, originZ, samples, mask, floodLevel);
    }

    private void buildMask(
            long seed,
            ChunkPos target,
            int originX,
            int originZ,
            TerrainSample[] samples,
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
                        carveCavePath(caves, sourceX, sourceZ, originX, originZ, samples, mask);
                    }
                }

                SplittableRandom ravines = new SplittableRandom(sourceSeed ^ RAVINE_SALT);
                if (ravines.nextDouble() < RAVINE_ORIGIN_CHANCE) {
                    carveRavinePath(ravines, sourceX, sourceZ, originX, originZ, samples, mask);
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
            TerrainSample[] samples,
            boolean[] mask) {
        double x = sourceChunkX * 16.0D + random.nextDouble(16.0D);
        double z = sourceChunkZ * 16.0D + random.nextDouble(16.0D);
        double y = randomCarveY(random, 14, 88);
        double yaw = random.nextDouble(Math.PI * 2.0D);
        double pitch = (random.nextDouble() - 0.5D) * 0.22D;
        int length = 26 + random.nextInt(38);
        double baseRadius = 1.55D + random.nextDouble() * 2.25D;

        for (int step = 0; step < length; step++) {
            double progress = (step + 0.5D) / length;
            double profile = 0.62D + Math.sin(progress * Math.PI) * 0.58D;
            double horizontal = baseRadius * profile;
            double vertical = horizontal * (0.70D + random.nextDouble() * 0.18D);
            markEllipsoid(x, y, z, horizontal, vertical, originX, originZ, samples, mask);

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
            TerrainSample[] samples,
            boolean[] mask) {
        double x = sourceChunkX * 16.0D + random.nextDouble(16.0D);
        double z = sourceChunkZ * 16.0D + random.nextDouble(16.0D);
        double y = randomCarveY(random, 18, 72);
        double yaw = random.nextDouble(Math.PI * 2.0D);
        double pitch = (random.nextDouble() - 0.5D) * 0.08D;
        int length = 48 + random.nextInt(42);
        double baseWidth = 2.4D + random.nextDouble() * 2.2D;

        for (int step = 0; step < length; step++) {
            double progress = (step + 0.5D) / length;
            double profile = 0.55D + Math.sin(progress * Math.PI) * 0.75D;
            double horizontal = baseWidth * profile;
            double vertical = horizontal * (1.35D + random.nextDouble() * 0.35D);
            markEllipsoid(x, y, z, horizontal, vertical, originX, originZ, samples, mask);

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
            TerrainSample[] samples,
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
                TerrainSample sample = samples[column];
                int surfaceY = materializer.solidSurfaceY(sample);
                int columnMaxY = Math.min(maxY, surfaceY);
                for (int y = minY; y <= columnMaxY; y++) {
                    double dy = (y + 0.5D - centerY) * inverseY;
                    if (horizontal + dy * dy < 1.0D) {
                        mask[pack(column, y)] = true;
                    }
                }
            }
        }
    }

    private int[] resolveConnectedWater(
            int originX,
            int originZ,
            TerrainSample[] samples,
            boolean[] mask) {
        int[] floodLevel = new int[mask.length];
        int[] hydraulicCeiling = new int[AREA];
        Arrays.fill(floodLevel, Integer.MIN_VALUE);
        Arrays.fill(hydraulicCeiling, context.maxYExclusive());
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int localZ = 0; localZ < WIDTH; localZ++) {
            int z = originZ + localZ;
            for (int localX = 0; localX < WIDTH; localX++) {
                int x = originX + localX;
                int column = columnIndex(localX, localZ);
                TerrainSample sample = samples[column];
                if (!materializer.hasFinalWetEnvelope(sample, x, z)) {
                    continue;
                }
                int bedY = materializer.solidSurfaceY(sample);
                int waterTop = clamp(
                        materializer.waterTopExclusive(sample),
                        context.minY(),
                        context.maxYExclusive());
                hydraulicCeiling[column] = waterTop;
                if (bedY < context.minY()
                        || bedY >= context.maxYExclusive()
                        || waterTop <= bedY
                        || !mask[pack(column, bedY)]) {
                    continue;
                }
                offerFlood(queue, floodLevel, pack(column, bedY), waterTop);
            }
        }

        while (!queue.isEmpty()) {
            int packed = queue.removeFirst();
            int level = floodLevel[packed];
            int vertical = packed / AREA;
            int column = packed - vertical * AREA;
            int localX = column % WIDTH;
            int localZ = column / WIDTH;
            int y = context.minY() + vertical;

            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX - 1, localZ, y, level);
            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX + 1, localZ, y, level);
            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX, localZ - 1, y, level);
            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX, localZ + 1, y, level);
            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX, localZ, y - 1, level);
            offerNeighbor(queue, floodLevel, hydraulicCeiling, mask, localX, localZ, y + 1, level);
        }
        return floodLevel;
    }

    private void offerNeighbor(
            ArrayDeque<Integer> queue,
            int[] floodLevel,
            int[] hydraulicCeiling,
            boolean[] mask,
            int localX,
            int localZ,
            int y,
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
        offerFlood(queue, floodLevel, packed, cappedLevel);
    }

    private static void offerFlood(
            ArrayDeque<Integer> queue,
            int[] floodLevel,
            int packed,
            int level) {
        if (level <= floodLevel[packed]) {
            return;
        }
        floodLevel[packed] = level;
        queue.addLast(packed);
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
                int surfaceY = Math.min(
                        context.maxYExclusive() - 2,
                        materializer.solidSurfaceY(sample));
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
                        target = materializer.fluidState(sample);
                    } else if (!current.getFluidState().isEmpty()) {
                        // Preserve pre-existing aquifer/lava cells that are not part of a connected
                        // Engine surface-water body. The owned carver controls geometry without
                        // destroying independent underground fluids supplied by the noise stage.
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
}
