# Minecraft 1.20.1 functional world-generation binding


> **Runtime dependency (Fabric):** Install Fabric API `0.92.2+1.20.1` (or a compatible newer 1.20.1 release). Its `fabric-resource-loader-v0` module is required for FlTerraForged's bundled world-preset data pack to participate in the 1.20.1 worldgen registry reload.


Snapshot r12 turns the first Fabric binding from a column-only proof of concept
into a hybrid FlTerraForged/vanilla world-generation pipeline.

## Ownership boundary

FlTerraForged Engine owns:

1. continent layout,
2. terrain regions and fractional surface height,
3. erosion,
4. river/rivermap signals,
5. climate and native-biome routing.

Minecraft 1.20.1 owns:

1. the `minecraft:overworld` NoiseRouter density substrate,
2. vanilla aquifers and ore-vein substrate generated during noise fill,
3. the configured vanilla surface-rule graph,
4. AIR and LIQUID carvers,
5. biome generation settings, placed features and ores,
6. biome mob population and the generic structure pipeline.

The bridge deliberately does not copy Mojang density, aquifer, carver or feature
algorithms into FlTerraForged.

## Generation order

```text
NoiseChunkGenerator.populateNoise
        |
        | vanilla NoiseRouter + aquifers
        v
EngineDensityBridge.reshape
        |
        | vertically remap each 3D column to TerrainWorld.surfaceHeight
        v
NoiseChunkGenerator.buildSurface
        |
        | minecraft:overworld Surface Rules
        v
EngineSurfaceGuard
        |
        | river/coast/snow semantics + fallback top/filler
        v
NoiseChunkGenerator.carve
        |
        | AIR / LIQUID caves
        v
ChunkGenerator.generateFeatures (inherited)
        |
        | ores, vegetation, springs, disks, etc. from selected native biomes
        v
NoiseChunkGenerator.populateEntities
```

## Density bridge

`EngineDensityBridge` does not replace the vanilla NoiseRouter. It uses its
already generated 3D block volume as a substrate.

For each x/z column it:

1. snapshots the vanilla density-filled column,
2. finds the highest solid density boundary,
3. samples FlTerraForged's target surface height,
4. computes a vertical delta,
5. remaps the complete underground vanilla column by that delta,
6. preserves underground air and fluid pockets,
7. restores the configured sea/river water above the final engine surface.

This keeps a usable 3D cave/aquifer substrate while making the externally
replaceable engine authoritative for the visible large-scale terrain shape.

## Surface rules

The normal `minecraft:overworld` `ChunkGeneratorSettings.surfaceRule()` remains
active through the vanilla `NoiseChunkGenerator` delegate. Because a vertically
remapped surface can differ substantially from the preliminary vanilla density
height, `EngineSurfaceGuard` runs afterward.

The guard is intentionally narrow:

- engine coast/river surfaces are sand,
- cold elevated surfaces can be snow,
- if vanilla left the exact engine surface as the default stone block, a
  grass/dirt or sand fallback is applied.

Existing non-default blocks produced by vanilla surface rules are preserved.

## Carvers, aquifers, ores and features

Aquifers are created as part of the delegated vanilla noise fill and are then
vertically remapped with the surrounding density column.

`carve(...)` is delegated directly to the vanilla noise generator with the same
biome source and settings. This restores vanilla AIR/LIQUID carvers.

FlTerraForged does not override `ChunkGenerator.generateFeatures()`. The base
implementation therefore continues to use the native biome generation settings
exposed by `FlTerraForgedBiomeSource`. Ores and biome placed features remain a
Minecraft-owned stage.

## Known boundary

This is a hybrid adapter rather than a replacement Mojang DensityFunction graph.
The vanilla 3D substrate is vertically warped per x/z column, so very steep
engine terrain can bend cave/aquifer geometry. That trade-off is intentional for
the first complete 1.20.1 reference binding: it gives a functional vanilla
underground without introducing Minecraft classes into the Engine API.

A later family-level density integration may inject an engine surface term into
the version-specific density graph while keeping the external Engine API
unchanged.
