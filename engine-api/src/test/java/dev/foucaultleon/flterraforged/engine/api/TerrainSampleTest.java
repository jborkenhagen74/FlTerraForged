package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import org.junit.jupiter.api.Test;

class TerrainSampleTest {

    @Test
    void preservesFractionalSurfaceHeight() {
        TerrainSample sample = TerrainSample.minimal(123.625D);
        assertEquals(123.625D, sample.surfaceHeight());
        assertTrue(sample.hasFractionalHeight());
        assertFalse(sample.hasSlope());
    }
}
