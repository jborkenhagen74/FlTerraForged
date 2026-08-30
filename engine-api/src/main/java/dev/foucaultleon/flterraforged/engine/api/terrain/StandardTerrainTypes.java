package dev.foucaultleon.flterraforged.engine.api.terrain;

/** Common terrain semantics understood by the default FlTerraForged adapters. */
public final class StandardTerrainTypes {

    /** Namespace used by all standard terrain identifiers. */
    public static final String NAMESPACE = "flterraforged";

    /** Unknown or unclassified terrain. */
    public static final TerrainType UNKNOWN = type("unknown");
    /** Ocean terrain. */
    public static final TerrainType OCEAN = type("ocean");
    /** Coastal terrain. */
    public static final TerrainType COAST = type("coast");
    /** Plains terrain. */
    public static final TerrainType PLAINS = type("plains");
    /** Hill terrain. */
    public static final TerrainType HILLS = type("hills");
    /** Mountain terrain. */
    public static final TerrainType MOUNTAINS = type("mountains");
    /** Plateau terrain. */
    public static final TerrainType PLATEAU = type("plateau");
    /** Valley terrain. */
    public static final TerrainType VALLEY = type("valley");
    /** River terrain. */
    public static final TerrainType RIVER = type("river");
    /** Lake terrain. */
    public static final TerrainType LAKE = type("lake");

    private StandardTerrainTypes() {
    }

    private static TerrainType type(String value) {
        return TerrainType.of(NAMESPACE, value);
    }
}
