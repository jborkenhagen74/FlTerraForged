package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import dev.foucaultleon.flterraforged.engine.api.climate.ClimateSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.StandardTerrainTypes;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainSample;
import dev.foucaultleon.flterraforged.engine.api.terrain.TerrainType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

/** Maps engine semantics to a small native Minecraft biome palette. */
public final class NativeBiomeRouter {

    private NativeBiomeRouter() {
    }

    /**
     * Resolves a Minecraft biome from the engine's terrain and climate signals.
     *
     * @param sample external-engine terrain sample
     * @param palette configured native biome palette
     * @return selected Minecraft biome entry
     */
    public static RegistryEntry<Biome> route(TerrainSample sample, BiomePalette palette) {
        TerrainType terrain = sample.terrainType();
        if (StandardTerrainTypes.OCEAN.equals(terrain)) {
            return palette.ocean();
        }
        if (StandardTerrainTypes.COAST.equals(terrain)) {
            return palette.coast();
        }
        if (StandardTerrainTypes.RIVER.equals(terrain)
                || StandardTerrainTypes.LAKE.equals(terrain)) {
            return palette.river();
        }

        ClimateSample climate = sample.climate();
        double temperature = climate.isAvailable() ? climate.temperature() : 0.5;
        double moisture = climate.isAvailable() ? climate.moisture() : 0.5;

        if (temperature < 0.22) {
            return palette.snowy();
        }
        if (RiparianZone.isDryBank(sample)) {
            return palette.plains();
        }
        if (temperature > 0.72 && moisture < 0.38) {
            return palette.desert();
        }
        if (temperature > 0.68 && moisture > 0.64) {
            return palette.jungle();
        }
        if (StandardTerrainTypes.MOUNTAINS.equals(terrain)
                || StandardTerrainTypes.PLATEAU.equals(terrain)) {
            return palette.mountains();
        }
        if (moisture > 0.62) {
            return palette.forest();
        }
        return palette.plains();
    }
}
