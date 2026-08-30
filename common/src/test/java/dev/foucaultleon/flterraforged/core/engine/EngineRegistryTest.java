package dev.foucaultleon.flterraforged.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.foucaultleon.flterraforged.engine.api.EngineApiVersion;
import dev.foucaultleon.flterraforged.engine.api.EngineConfig;
import dev.foucaultleon.flterraforged.engine.api.EngineId;
import dev.foucaultleon.flterraforged.engine.api.EngineProvider;
import dev.foucaultleon.flterraforged.engine.api.TerrainEngine;
import org.junit.jupiter.api.Test;

class EngineRegistryTest {

    @Test
    void rejectsDuplicateProviderIds() {
        EngineRegistry registry = new EngineRegistry();
        registry.register(new StubProvider());
        assertThrows(IllegalStateException.class, () -> registry.register(new StubProvider()));
        assertEquals(1, registry.providers().size());
    }

    private static final class StubProvider implements EngineProvider {

        @Override
        public EngineId id() {
            return EngineId.of("test", "engine");
        }

        @Override
        public String displayName() {
            return "Test Engine";
        }

        @Override
        public String engineVersion() {
            return "0";
        }

        @Override
        public EngineApiVersion apiVersion() {
            return EngineApiVersion.CURRENT;
        }

        @Override
        public TerrainEngine create(EngineConfig config) {
            throw new UnsupportedOperationException();
        }
    }
}
