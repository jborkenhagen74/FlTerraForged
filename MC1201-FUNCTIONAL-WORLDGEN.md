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
2. vanilla 3D cave/density and ore-vein substrate generated during noise fill,
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
        | vanilla NoiseRouter + preliminary aquifers
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
5. remaps solid blocks and cave-space geometry by that delta,
6. does not vertically translate underground water/lava cells,
7. reconstructs only stable global sea-level water above the final engine surface.

The earlier r12-r18 bridge moved aquifer fluids with the terrain column. Large
height deltas could therefore lift deep lava toward spawn height and expose huge
fluid fronts that caused chained neighbor-update cascades. r19 uses a conservative
stability baseline: caves remain, but translated underground aquifer fluids are
removed until a dedicated height-stable aquifer adapter is implemented.

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

Vanilla still creates its preliminary aquifer states during delegated noise fill,
but r19 deliberately does not translate those fluid blocks with the Engine height
delta. Full underground aquifer-fluid restoration is deferred to a dedicated
Minecraft-side adapter that can keep water/lava levels stable in absolute world Y.

`carve(...)` is delegated directly to the vanilla noise generator with the same
biome source and settings. This restores vanilla AIR/LIQUID carvers.

FlTerraForged does not override `ChunkGenerator.generateFeatures()`. The base
implementation therefore continues to use the native biome generation settings
exposed by `FlTerraForgedBiomeSource`. Ores and biome placed features remain a
Minecraft-owned stage.

## Known boundary

This is a hybrid adapter rather than a replacement Mojang DensityFunction graph.
The vanilla 3D substrate is vertically warped per x/z column, so very steep
engine terrain can bend cave geometry. Underground aquifer fluids are temporarily
suppressed by the safe remap because moving absolute fluid levels proved unstable.
This keeps the reference binding testable without introducing Minecraft classes
into the Engine API.

A later family-level density integration may inject an engine surface term into
the version-specific density graph while keeping the external Engine API
unchanged.
