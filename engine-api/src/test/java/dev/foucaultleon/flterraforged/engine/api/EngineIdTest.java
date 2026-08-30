package dev.foucaultleon.flterraforged.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EngineIdTest {

    @Test
    void parsesNamespacedId() {
        EngineId id = EngineId.parse("flterraforged:default");
        assertEquals("flterraforged", id.namespace());
        assertEquals("default", id.value());
    }

    @Test
    void rejectsInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> EngineId.parse("MissingNamespace"));
    }
}
