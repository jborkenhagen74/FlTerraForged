package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Optional;
import net.minecraft.block.BlockState;

/**
 * Converts continuous Engine terrain semantics into Minecraft 1.20.1 block-level decisions.
 *
 * <p>The Engine never depends on this contract. FlTerraForged owns materialization and supplies the
 * selected implementation to every world-generation stage that needs integer heights, fluids or
 * surface blocks. Add-on mods may provide another implementation through the
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
     * Returns whether at least one Engine-owned river/lake water cell is materialized above the
     * solid bed.
     *
     * @param sample continuous Engine sample
     * @return {@code true} when Engine inland hydrology becomes actual Minecraft water
     */
    boolean hasMaterializedWater(TerrainSample sample);

    /**
     * Returns whether the column participates in the final physical wet envelope.
     *
     * <p>R52 distinguishes a complete fluid cell above the physical surface from water that would
     * share the provider's partial-height top cell. The latter is considered materializable only when
     * the provider advertises waterlogging. A non-waterloggable custom slab/layer is therefore never
     * replaced by a full fluid block merely because an integer water top overlaps its block cell.</p>
     *
     * @param sample continuous Engine sample
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return {@code true} when the final water envelope can be physically represented
     */
    default boolean hasFinalWetEnvelope(TerrainSample sample, int x, int z) {
        MaterializedSurfaceGeometry geometry = MaterializerGeometry.surfaceGeometry(this, sample, x, z);
        return MaterializerGeometry.hasMaterializableWater(
                this,
                geometry,
                waterTopExclusive(sample));
    }

    /**
     * Returns whether the final wet pass may propagate through the current block cell.
     *
     * <p>The full-block default permits air and the materializer's own fluid only. A provider that
     * emits waterloggable partial blocks may override this method together with
     * {@link #finalWetState(TerrainSample, BlockState, int, int, int)} so connected water can occupy
     * its custom geometry without the host replacing that geometry with a full fluid block.</p>
     *
     * @param sample continuous Engine sample for the column
     * @param current current Minecraft block state
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return {@code true} when final wet connectivity may traverse this block
     */
    default boolean permitsFinalWetFlow(
            TerrainSample sample,
            BlockState current,
            int x,
            int y,
            int z) {
        if (current.isAir()) {
            return true;
        }
        BlockState fluid = fluidState(sample);
        return !current.getFluidState().isEmpty()
                && !fluid.getFluidState().isEmpty()
                && current.getFluidState().getFluid() == fluid.getFluidState().getFluid();
    }

    /**
     * Resolves the block state used by the one-time final wet reconciliation pass.
     *
     * <p>This method is called only after {@link #permitsFinalWetFlow(TerrainSample, BlockState,
     * int, int, int)} returned {@code true}. The default returns the configured full fluid state.
     * Waterloggable partial-height providers can instead retain their solid state while enabling
     * its waterlogged property.</p>
     *
     * @param sample continuous Engine sample for the column
     * @param current current Minecraft block state
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return state that represents connected final water in this cell
     */
    default BlockState finalWetState(
            TerrainSample sample,
            BlockState current,
            int x,
            int y,
            int z) {
        return fluidState(sample);
    }

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
     * Returns the legacy visible bed state retained for source compatibility with older add-ons.
     *
     * <p>R45 no longer reconstructs solid beds after carving; the final wet pass only adds fluid to
     * connected wet cells.</p>
     *
     * @param sample continuous Engine sample
     * @return hydrology bed state
     */
    BlockState hydrologyBedState(TerrainSample sample);

    /**
     * Returns the legacy hydrology-bed state for a known block position.
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
     * Returns the legacy hydrology seal state retained for source compatibility.
     *
     * @param sample continuous Engine sample
     * @return hydrology seal state
     */
    BlockState hydrologySealState(TerrainSample sample);

    /**
     * Returns the legacy hydrology seal state for a known block position.
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
     * Returns whether legacy inferred gap repair would be allowed.
     *
     * <p>R45 does not perform inferred gap repair. This method remains only for compatibility with
     * existing materializer implementations.</p>
     *
     * @param sample continuous Engine sample at the candidate gap
     * @return legacy gap-repair permission
     */
    default boolean mayRepairHydrologyGap(TerrainSample sample) {
        return false;
    }

    /**
     * Resolves the legacy inferred-gap bed Y retained for compatibility.
     *
     * @param sample continuous Engine sample at the candidate gap
     * @param waterTopExclusive neighborhood water-top level
     * @return legacy bed Y
     */
    default int hydrologyGapBedY(TerrainSample sample, int waterTopExclusive) {
        return solidSurfaceY(sample);
    }

    /**
     * Returns the legacy gap-repair radius retained for compatibility.
     *
     * @return legacy gap-repair radius
     */
    default int hydrologyGapRepairRadius() {
        return 0;
    }

    /**
     * Returns the legacy cave-protection margin retained for compatibility.
     *
     * @return legacy protection radius
     */
    default int hydrologyCaveMargin() {
        return 0;
    }

    /**
     * Returns the legacy bed seal depth retained for compatibility.
     *
     * @return legacy bed seal depth
     */
    default int hydrologyBedSealDepth() {
        return 1;
    }

    /**
     * Returns the legacy bank seal depth retained for compatibility.
     *
     * @return legacy bank seal depth
     */
    default int hydrologyBankSealDepth() {
        return 1;
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
