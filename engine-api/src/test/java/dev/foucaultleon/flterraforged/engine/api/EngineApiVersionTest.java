package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EngineApiVersionTest {

    @Test
    void comparesSemanticComponents() {
        assertTrue(new EngineApiVersion(0, 2, 0).compareTo(new EngineApiVersion(0, 1, 9)) > 0);
        assertEquals("0.1.0", EngineApiVersion.CURRENT.toString());
    }
}
