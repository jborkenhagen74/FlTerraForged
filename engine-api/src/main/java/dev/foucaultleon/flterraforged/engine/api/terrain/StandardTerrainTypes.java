package dev.foucaultleon.flterraforged.engine.api.terrain;

/** Common terrain semantics understood by the default FlTerraForged adapters. */
public final class StandardTerrainTypes {

    public static final String NAMESPACE = "flterraforged";

    public static final TerrainType UNKNOWN = type("unknown");
    public static final TerrainType OCEAN = type("ocean");
    public static final TerrainType COAST = type("coast");
    public static final TerrainType PLAINS = type("plains");
    public static final TerrainType HILLS = type("hills");
    public static final TerrainType MOUNTAINS = type("mountains");
    public static final TerrainType PLATEAU = type("plateau");
    public static final TerrainType VALLEY = type("valley");
    public static final TerrainType RIVER = type("river");
    public static final TerrainType LAKE = type("lake");

    private StandardTerrainTypes() {
    }

    private static TerrainType type(String value) {
        return TerrainType.of(NAMESPACE, value);
    }
}
