package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.NaturalMaterialResolver;
import dev.foucaultleon.flterraforged.engine.api.chunk.ChunkSnapshot;
import dev.foucaultleon.flterraforged.engine.api.chunk.ColumnSnapshot;
import dev.foucaultleon.flterraforged.engine.api.chunk.NaturalMaterial;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.NaturalMaterialFallback;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Writes one complete immutable Engine chunk snapshot into a Minecraft chunk.
 *
 * <p>This class performs geometry traversal and materializer delegation only. It never asks any
 * Vanilla noise, surface, carver or aquifer system for natural geometry. Optional
 * {@link NaturalMaterialResolver} implementations may map Engine geology and fractional surface
 * metadata to custom providers such as Conquest Reforged without moving geometry ownership out of
 * the Engine.</p>
 */
public final class EngineChunkMaterializer {

    private final BlockMaterializer materializer;
    private final MaterializerContext context;
    private final NaturalMaterialResolver resolver;

    /**
     * Creates a direct Engine snapshot materializer.
     *
     * @param materializer active replaceable Minecraft block materializer
     */
    public EngineChunkMaterializer(BlockMaterializer materializer) {
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.context = materializer.context();
        this.resolver = materializer instanceof NaturalMaterialResolver extension ? extension : null;
    }

    /**
     * Materializes the complete natural block volume for one chunk.
     *
     * <p>Engine snapshots are dense in storage but natural columns cannot contain material above
     * the higher of their solid surface and water surface. The target proto-chunk is air before
     * this stage, so R59 does not traverse that guaranteed-air upper volume and does not resolve or
     * write cave-air cells. This removes most block-state lookups from ordinary lowland chunks while
     * preserving the exact Engine-owned geometry below the natural top.</p>
     *
     * @param chunk target Minecraft chunk
     * @param snapshot immutable Engine-owned natural chunk snapshot
     * @throws IllegalArgumentException when the Engine snapshot does not match the target chunk or
     *     configured vertical range
     */
    public void materialize(Chunk chunk, ChunkSnapshot snapshot) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(snapshot, "snapshot");
        ChunkPos pos = chunk.getPos();
        if (pos.x != snapshot.chunkX() || pos.z != snapshot.chunkZ()) {
            throw new IllegalArgumentException("Engine snapshot does not match target chunk");
        }
        if (snapshot.minY() != context.minY()
                || snapshot.maxYExclusive() != context.maxYExclusive()) {
            throw new IllegalArgumentException("Engine snapshot vertical range does not match materializer context");
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int localZ = 0; localZ < ChunkSnapshot.WIDTH; localZ++) {
            int blockZ = pos.getStartZ() + localZ;
            for (int localX = 0; localX < ChunkSnapshot.WIDTH; localX++) {
                int blockX = pos.getStartX() + localX;
                ColumnSnapshot column = snapshot.column(localX, localZ);
                int topY = Math.max(column.solidSurfaceY(), column.waterTopExclusive() - 1);
                topY = Math.min(topY, snapshot.maxYExclusive() - 1);
                for (int y = snapshot.minY(); y <= topY; y++) {
                    NaturalMaterial natural = snapshot.materialAt(localX, y, localZ);
                    if (natural == NaturalMaterial.AIR) {
                        continue;
                    }
                    BlockState target = resolve(column, natural, blockX, y, blockZ);
                    mutable.set(blockX, y, blockZ);
                    BlockState current = chunk.getBlockState(mutable);
                    if (!target.equals(current)) {
                        chunk.setBlockState(mutable, target, false);
                    }
                }
            }
        }
    }

    private BlockState resolve(
            ColumnSnapshot column,
            NaturalMaterial natural,
            int x,
            int y,
            int z) {
        if (resolver != null) {
            BlockState resolved = resolver.resolveNaturalMaterial(column, natural, x, y, z);
            if (resolved != null) {
                return resolved;
            }
        }
        return NaturalMaterialFallback.resolve(materializer, column, natural, x, y, z);
    }
}
