package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.river.RiverSample;
import org.junit.jupiter.api.Test;

final class RiverSampleCompatibilityTest {

    @Test
    void legacyConstructorKeepsCoreHydrologyAvailable() {
        RiverSample sample = new RiverSample(2.0D, 8.0D, 1.5D);
        assertTrue(sample.isAvailable());
        assertFalse(sample.hasWaterSurfaceHeight());
        assertFalse(sample.hasFlow());
    }

    @Test
    void extendedSampleExposesWaterSurfaceAndFlow() {
        RiverSample sample = new RiverSample(1.0D, 10.0D, 2.0D, 72.5D, 18.0D);
        assertTrue(sample.isAvailable());
        assertTrue(sample.hasWaterSurfaceHeight());
        assertTrue(sample.hasFlow());
    }
}
