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
import net.minecraft.world.chunk.ChunkSection;

/** Writes one complete immutable Engine chunk snapshot into a Minecraft chunk. */
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
     * <p>Fresh proto-chunks contain air before this stage. R60 therefore writes ordinary natural
     * states directly through {@link ChunkSection}, avoiding a redundant block lookup and global
     * position dispatch for every stone/soil/water voxel. Blocks requiring host bookkeeping, such
     * as block entities or luminant states, retain the safe chunk-level write path. Heightmaps are
     * rebuilt after the complete snapshot has been materialized.</p>
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
                int currentSectionIndex = Integer.MIN_VALUE;
                ChunkSection currentSection = null;
                for (int y = snapshot.minY(); y <= topY; y++) {
                    NaturalMaterial natural = snapshot.materialAt(localX, y, localZ);
                    if (natural == NaturalMaterial.AIR) {
                        continue;
                    }
                    BlockState target = resolve(column, natural, blockX, y, blockZ);
                    if (requiresChunkWrite(target)) {
                        mutable.set(blockX, y, blockZ);
                        chunk.setBlockState(mutable, target, false);
                        continue;
                    }
                    int sectionIndex = chunk.getSectionIndex(y);
                    if (sectionIndex != currentSectionIndex) {
                        currentSectionIndex = sectionIndex;
                        currentSection = chunk.getSection(sectionIndex);
                    }
                    currentSection.setBlockState(localX, y & 15, localZ, target, false);
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

    private static boolean requiresChunkWrite(BlockState state) {
        return state.hasBlockEntity() || state.getLuminance() > 0;
    }
}
