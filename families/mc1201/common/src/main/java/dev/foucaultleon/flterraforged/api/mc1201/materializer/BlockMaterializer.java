package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Optional;
import net.minecraft.block.BlockState;

/**
 * Converts continuous Engine terrain semantics into Minecraft 1.20.1 block-level decisions.
 *
 * <p>The Engine never depends on this contract. FlTerraForged owns materialization and supplies the
 * selected implementation to every world-generation stage that needs integer heights, fluids,
 * surface blocks or hydrology repair. Add-on mods may provide another implementation through the
 * {@code flterraforged:materializer} Fabric entrypoint.</p>
 */
public interface BlockMaterializer {

    /**
     * Returns the immutable generation context used by this instance.
     *
     * @return materializer context
     */
    MaterializerContext context();

    /**
     * Returns the implementation capabilities.
     *
     * @return materializer capabilities
     */
    MaterializerCapabilities capabilities();

    /**
     * Resolves the topmost solid block Y for one Engine sample.
     *
     * @param sample continuous Engine sample
     * @return topmost solid block Y
     */
    int solidSurfaceY(TerrainSample sample);

    /**
     * Returns the first Y above the materialized solid surface.
     *
     * @param sample continuous Engine sample
     * @return first block above the solid surface
     */
    default int solidSurfaceTop(TerrainSample sample) {
        return solidSurfaceY(sample) + 1;
    }

    /**
     * Returns the first Y above ocean or Engine-owned hydrology water.
     *
     * @param sample continuous Engine sample
     * @return exclusive water top
     */
    int waterTopExclusive(TerrainSample sample);

    /**
     * Returns whether at least one Engine-owned water cell is materialized above the solid bed.
     *
     * @param sample continuous Engine sample
     * @return {@code true} when Engine hydrology becomes actual Minecraft water
     */
    boolean hasMaterializedWater(TerrainSample sample);

    /**
     * Returns the world-floor block used when a source column does not already provide one.
     *
     * @param sample continuous Engine sample
     * @return floor block state
     */
    BlockState bedrockState(TerrainSample sample);

    /**
     * Returns the structural substrate used for newly raised terrain.
     *
     * @param sample continuous Engine sample
     * @return substrate block state
     */
    BlockState substrateState(TerrainSample sample);

    /**
     * Returns the solid state used for the pre-carver surface seal.
     *
     * @param sample continuous Engine sample
     * @return surface-seal block state
     */
    BlockState surfaceSealState(TerrainSample sample);

    /**
     * Returns the air state used above materialized terrain and fluids.
     *
     * @param sample continuous Engine sample
     * @return air block state
     */
    BlockState airState(TerrainSample sample);

    /**
     * Returns the fluid state used for ocean and Engine hydrology cells.
     *
     * @param sample continuous Engine sample
     * @return fluid block state
     */
    BlockState fluidState(TerrainSample sample);

    /**
     * Returns the deterministic top state used by synchronous column composition.
     *
     * @param sample continuous Engine sample
     * @return composed surface block state
     */
    BlockState composedTopState(TerrainSample sample);

    /**
     * Returns the deterministic top state for a known world column.
     *
     * <p>The position-aware overload lets palettes vary naturally between neighboring columns.
     * Existing materializers remain source-compatible because the default implementation forwards
     * to the original sample-only method.</p>
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return composed surface block state
     */
    default BlockState composedTopState(TerrainSample sample, int x, int z) {
        return composedTopState(sample);
    }

    /**
     * Returns the deterministic filler state below a materialized surface.
     *
     * @param sample continuous Engine sample
     * @return filler block state
     */
    BlockState fillerState(TerrainSample sample);

    /**
     * Returns a deterministic filler state for a known block position.
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return filler block state
     */
    default BlockState fillerState(TerrainSample sample, int x, int y, int z) {
        return fillerState(sample);
    }

    /**
     * Returns a surface block that must override vanilla surface rules for the supplied semantics.
     *
     * <p>An empty value keeps the vanilla surface result unless a fallback is required.</p>
     *
     * @param sample continuous Engine sample
     * @return forced surface state, or empty when vanilla owns the surface
     */
    Optional<BlockState> forcedSurfaceState(TerrainSample sample);

    /**
     * Returns a forced surface state for a known world column.
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return forced surface state, or empty when vanilla owns the surface
     */
    default Optional<BlockState> forcedSurfaceState(TerrainSample sample, int x, int z) {
        return forcedSurfaceState(sample);
    }

    /**
     * Returns a deterministic surface fallback when vanilla left raw substrate at the Engine top.
     *
     * @param sample continuous Engine sample
     * @return fallback surface state
     */
    BlockState fallbackSurfaceState(TerrainSample sample);

    /**
     * Returns the deterministic fallback surface for a known world column.
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return fallback surface state
     */
    default BlockState fallbackSurfaceState(TerrainSample sample, int x, int z) {
        return fallbackSurfaceState(sample);
    }

    /**
     * Returns the visible bed state restored below Engine-owned river/lake water after carving.
     *
     * @param sample continuous Engine sample
     * @return hydrology bed state
     */
    BlockState hydrologyBedState(TerrainSample sample);

    /**
     * Returns the visible hydrology-bed state for a known block position.
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return hydrology bed state
     */
    default BlockState hydrologyBedState(TerrainSample sample, int x, int y, int z) {
        return hydrologyBedState(sample);
    }

    /**
     * Returns the solid state used to seal hydrology beds and adjacent banks after carving.
     *
     * @param sample continuous Engine sample
     * @return hydrology seal state
     */
    BlockState hydrologySealState(TerrainSample sample);

    /**
     * Returns the hydrology seal state for a known block position.
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return hydrology seal state
     */
    default BlockState hydrologySealState(TerrainSample sample, int x, int y, int z) {
        return hydrologySealState(sample);
    }


    /**
     * Returns whether a dry Engine hydrology column may be repaired as a one-column surface gap
     * when surrounding columns form one continuous water body.
     *
     * <p>The host performs only the neighborhood/connectivity test. The selected materializer
     * decides whether its block-resolution model permits the repair at all.</p>
     *
     * @param sample continuous Engine sample at the candidate gap
     * @return {@code true} when a connectivity repair is allowed
     */
    default boolean mayRepairHydrologyGap(TerrainSample sample) {
        return false;
    }

    /**
     * Resolves the bed Y used when closing a one-column hydrology surface gap.
     *
     * @param sample continuous Engine sample at the candidate gap
     * @param waterTopExclusive neighborhood water-top level chosen by the host
     * @return topmost solid bed block Y for the repaired column
     */
    default int hydrologyGapBedY(TerrainSample sample, int waterTopExclusive) {
        return solidSurfaceY(sample);
    }

    /**
     * Returns the maximum radius used to prove and close a narrow enclosed hydrology gap.
     *
     * <p>A value of zero disables inferred gap repair. Exact Engine-owned wet columns are restored
     * independently of this setting.</p>
     *
     * @return maximum horizontal gap-repair radius in blocks
     */
    default int hydrologyGapRepairRadius() {
        return 1;
    }

    /**
     * Returns the horizontal cave-protection margin around materialized hydrology.
     *
     * @return protection radius in blocks
     */
    default int hydrologyCaveMargin() {
        return 3;
    }

    /**
     * Returns the number of solid blocks maintained below a hydrology bed after carving.
     *
     * @return bed seal depth in blocks
     */
    default int hydrologyBedSealDepth() {
        return 5;
    }

    /**
     * Returns the vertical seal depth used on banks inside the cave-protection margin.
     *
     * @return bank seal depth in blocks
     */
    default int hydrologyBankSealDepth() {
        return 5;
    }

    /**
     * Applies optional family-specific vegetation, partial blocks, spray and small water structures.
     *
     * <p>The host invokes this hook after native biome features. The default is intentionally empty
     * so legacy and full-block-only providers remain source-compatible.</p>
     *
     * @param context writable chunk and semantic terrain context
     */
    default void decorateWatercourses(WaterDecorationContext context) {
        // Optional extension point.
    }
}
