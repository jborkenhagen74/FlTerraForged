package dev.foucaultleon.flterraforged.engine.api;

/** Optional data an engine can expose to FlTerraForged. */
public enum EngineCapability {
    /** Engine exposes continuous, non-integer surface heights. */
    FRACTIONAL_HEIGHT,
    /** Engine exposes terrain slope values. */
    SLOPE,
    /** Engine exposes erosion values. */
    EROSION,
    /** Engine exposes continentalness values. */
    CONTINENTALNESS,
    /** Engine exposes climate samples. */
    CLIMATE,
    /** Engine exposes river or hydrology samples. */
    RIVERS,
    /** Engine exposes semantic terrain types. */
    TERRAIN_TYPE
}
