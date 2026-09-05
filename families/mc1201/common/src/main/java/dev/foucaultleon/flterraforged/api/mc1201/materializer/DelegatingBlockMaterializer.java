package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.BlockState;

/**
 * Convenience base class for add-ons that override only selected materialization decisions.
 */
public abstract class DelegatingBlockMaterializer implements BlockMaterializer {

    private final BlockMaterializer delegate;

    /**
     * Creates a forwarding materializer.
     *
     * @param delegate materializer receiving all non-overridden calls
     */
    protected DelegatingBlockMaterializer(BlockMaterializer delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Returns the wrapped implementation.
     *
     * @return delegate materializer
     */
    protected final BlockMaterializer delegate() {
        return delegate;
    }

    @Override
    public MaterializerContext context() {
        return delegate.context();
    }

    @Override
    public MaterializerCapabilities capabilities() {
        return delegate.capabilities();
    }

    @Override
    public int solidSurfaceY(TerrainSample sample) {
        return delegate.solidSurfaceY(sample);
    }

    @Override
    public int waterTopExclusive(TerrainSample sample) {
        return delegate.waterTopExclusive(sample);
    }

    @Override
    public boolean hasMaterializedWater(TerrainSample sample) {
        return delegate.hasMaterializedWater(sample);
    }

    @Override
    public boolean hasFinalWetEnvelope(TerrainSample sample, int x, int z) {
        return delegate.hasFinalWetEnvelope(sample, x, z);
    }

    @Override
    public boolean permitsFinalWetFlow(
            TerrainSample sample,
            BlockState current,
            int x,
            int y,
            int z) {
        return delegate.permitsFinalWetFlow(sample, current, x, y, z);
    }

    @Override
    public BlockState finalWetState(
            TerrainSample sample,
            BlockState current,
            int x,
            int y,
            int z) {
        return delegate.finalWetState(sample, current, x, y, z);
    }

    @Override
    public BlockState bedrockState(TerrainSample sample) {
        return delegate.bedrockState(sample);
    }

    @Override
    public BlockState substrateState(TerrainSample sample) {
        return delegate.substrateState(sample);
    }

    @Override
    public BlockState surfaceSealState(TerrainSample sample) {
        return delegate.surfaceSealState(sample);
    }

    @Override
    public BlockState airState(TerrainSample sample) {
        return delegate.airState(sample);
    }

    @Override
    public BlockState fluidState(TerrainSample sample) {
        return delegate.fluidState(sample);
    }

    @Override
    public BlockState composedTopState(TerrainSample sample) {
        return delegate.composedTopState(sample);
    }

    @Override
    public BlockState composedTopState(TerrainSample sample, int x, int z) {
        return composedTopState(sample);
    }

    @Override
    public BlockState fillerState(TerrainSample sample) {
        return delegate.fillerState(sample);
    }

    @Override
    public BlockState fillerState(TerrainSample sample, int x, int y, int z) {
        return fillerState(sample);
    }

    @Override
    public Optional<BlockState> forcedSurfaceState(TerrainSample sample) {
        return delegate.forcedSurfaceState(sample);
    }

    @Override
    public Optional<BlockState> forcedSurfaceState(TerrainSample sample, int x, int z) {
        return forcedSurfaceState(sample);
    }

    @Override
    public BlockState fallbackSurfaceState(TerrainSample sample) {
        return delegate.fallbackSurfaceState(sample);
    }

    @Override
    public BlockState fallbackSurfaceState(TerrainSample sample, int x, int z) {
        return fallbackSurfaceState(sample);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample) {
        return delegate.hydrologyBedState(sample);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample, int x, int y, int z) {
        return hydrologyBedState(sample);
    }

    @Override
    public BlockState hydrologySealState(TerrainSample sample) {
        return delegate.hydrologySealState(sample);
    }

    @Override
    public BlockState hydrologySealState(TerrainSample sample, int x, int y, int z) {
        return hydrologySealState(sample);
    }

    @Override
    public boolean mayRepairHydrologyGap(TerrainSample sample) {
        return delegate.mayRepairHydrologyGap(sample);
    }

    @Override
    public int hydrologyGapBedY(TerrainSample sample, int waterTopExclusive) {
        return delegate.hydrologyGapBedY(sample, waterTopExclusive);
    }

    @Override
    public int hydrologyGapRepairRadius() {
        return delegate.hydrologyGapRepairRadius();
    }

    @Override
    public int hydrologyCaveMargin() {
        return delegate.hydrologyCaveMargin();
    }

    @Override
    public int hydrologyBedSealDepth() {
        return delegate.hydrologyBedSealDepth();
    }

    @Override
    public int hydrologyBankSealDepth() {
        return delegate.hydrologyBankSealDepth();
    }

    @Override
    public void decorateWatercourses(WaterDecorationContext context) {
        delegate.decorateWatercourses(context);
    }
}
