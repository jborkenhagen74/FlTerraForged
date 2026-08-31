package dev.foucaultleon.flterraforged.core;

import dev.foucaultleon.flterraforged.engine.api.EngineApiVersion;

/** Minecraft-independent FlTerraForged core metadata. */
public final class FlTerraForgedCore {

    /** Stable FlTerraForged namespace/mod identifier. */
    public static final String MOD_ID = "flterraforged";

    /** Engine API version compiled into this FlTerraForged host. */
    public static final EngineApiVersion ENGINE_API_VERSION = EngineApiVersion.CURRENT;

    private FlTerraForgedCore() {
    }
}
