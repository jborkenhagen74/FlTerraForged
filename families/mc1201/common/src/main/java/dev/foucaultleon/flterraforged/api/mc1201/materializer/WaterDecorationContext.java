package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.TerrainWorld;
import java.util.Objects;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

/**
 * Minecraft-1.20.1-family context supplied after native biome features have been generated.
 *
 * @param world writable world-generation view
 * @param chunk chunk whose watercourse decoration is being finalized
 * @param terrainWorld seed-bound semantic Engine terrain view
 */
public record WaterDecorationContext(
        StructureWorldAccess world,
        Chunk chunk,
        TerrainWorld terrainWorld) {

    /**
     * Creates a validated decoration context.
     *
     * @param world writable world-generation view
     * @param chunk chunk being finalized
     * @param terrainWorld semantic Engine terrain view
     */
    public WaterDecorationContext {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(terrainWorld, "terrainWorld");
    }
}
