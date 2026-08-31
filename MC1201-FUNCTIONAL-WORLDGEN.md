# Minecraft 1.20.1 functional world-generation binding


> **Runtime dependency (Fabric):** Install Fabric API `0.92.2+1.20.1` (or a compatible newer 1.20.1 release). Its `fabric-resource-loader-v0` module is required for FlTerraForged's bundled world-preset data pack to participate in the 1.20.1 worldgen registry reload.


Snapshot r23 keeps the r20 absolute-Y substrate model and extends the stable
Minecraft realization of Engine-owned river water levels.

## Ownership boundary

FlTerraForged Engine owns:

1. continent layout,
2. terrain regions and fractional surface height,
3. erosion,
4. river/rivermap signals including continuous river-water elevation and accumulated flow,
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
        | keep vanilla substrate at absolute Y; truncate/extend to Engine surface
        | enforce a 6-block solid surface skin
        | fill Engine river channels to the directed-segment water surface
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

`EngineDensityBridge` still uses the already generated vanilla 3D volume as a
substrate, but **never moves it vertically**.

For each x/z column it:

1. snapshots the vanilla density-filled column,
2. finds the highest solid vanilla boundary,
3. samples FlTerraForged's target surface height,
4. truncates everything above the Engine target surface,
5. fills newly raised terrain with the configured default solid block,
6. preserves vanilla caves, deepslate/noise substrate and aquifer states below
   the original surface at their original absolute Y coordinates,
7. enforces a six-block solid skin below the Engine surface before carvers,
8. reconstructs stable global sea-level water above low terrain,
9. materializes highland river water only from the Engine-provided directed-segment water surface.

The old r12-r19 algorithm computed a separate vertical delta for every x/z
column and translated the entire 3D profile. Because neighboring columns have
different deltas, continuous caves and stone layers were sheared into floating
platforms, broad horizontal gaps and near-vertical walls. r20 removes that
operation entirely.

The solid surface skin is deliberate: if the Engine target surface happens to
intersect a pre-existing vanilla cave, the base terrain is sealed first.
Minecraft's normal carver stage then creates cave mouths against the final
Engine terrain instead of inheriting accidental thin roofs from an unrelated
vanilla surface height.


## River, lake and riparian realization (r23)

The Engine now exposes two additive hydrology values through `RiverSample`:

- `waterSurfaceHeight`: continuous world-space Y of the nearest active channel water surface;
- `flow`: accumulated drainage weight of that channel segment.

Engine r18 keeps D8 only as the hidden drainage skeleton. A depression-fill/spill pass prevents local
minima from terminating watercourses, and meaningful basins become ponds/lakes with a shared spill
elevation. Visible stream and river centerlines are multi-point, terrain-guided paths rather than raw
D8 axis/diagonal segments.

The water surface remains Engine-owned and downstream-monotonic. Channel incision reserves a wet core
and the Engine deepens the actual eroded local bed when a small terrain hump would otherwise interrupt
water. `HydrologyColumn` is still the single Minecraft 1.20.1 realization rule used by both
`EngineDensityBridge` and `ColumnComposer`; it materializes both channel and lake water only below the
Engine-provided water surface. Explicit waterfall/rapid shaping and a later 3D density-native
river/aquifer integration remain follow-up work.

Engine r18 also changes runoff accumulation: each drainage node contributes according to a pre-river
climate sample rather than a constant value. Hot/dry cells contribute only a small runoff weight, so
local desert streams are uncommon. Accumulated upstream flow is preserved, which means a sufficiently
large river sourced in wetter terrain may still continue through a desert instead of being cut off at
the biome boundary. Hydrology map padding increases to 16 drainage cells to reduce region-edge flow
resets and visible cutoffs.

The host adds a dry-climate riparian fringe outside the wet channel. `RiparianZone` derives its width
from channel width and accumulated flow. Inside that fringe `NativeBiomeRouter` selects the native
plains biome so vanilla grass/flower/tree feature generation has a vegetated biome context, and
`EngineSurfaceGuard` guarantees a grass-over-dirt bank surface even when the surrounding dry biome
would otherwise leave sand up to the water. The wet river/lake bed itself remains gravel.

## Surface rules

The normal `minecraft:overworld` `ChunkGeneratorSettings.surfaceRule()` remains
active through the vanilla `NoiseChunkGenerator` delegate. Because the Engine surface can still differ substantially from the preliminary
vanilla density height, `EngineSurfaceGuard` runs afterward.

The guard is intentionally narrow:

- coasts remain sand while wet river/lake beds use gravel instead of a radial sand override,
- dry-climate riverbanks inside the flow-scaled riparian fringe are grass over dirt and route to plains vegetation,
- cold elevated surfaces can be snow,
- if vanilla left the exact engine surface as the default stone block, a
  grass/dirt or sand fallback is applied.

Existing non-default blocks produced by vanilla surface rules are preserved.

## Carvers, aquifers, ores and features

Vanilla aquifer states remain at their **absolute world Y** coordinates. They are
no longer translated with terrain, so deep lava cannot be lifted toward spawn.
If Engine terrain is raised, the added upper mass is solid and contains no
synthetic aquifer until later vanilla stages modify it.

`carve(...)` is delegated directly to the vanilla noise generator with the same
biome source and settings. This restores vanilla AIR/LIQUID carvers against the
final Engine-shaped surface mass.

FlTerraForged does not override `ChunkGenerator.generateFeatures()`. The base
implementation therefore continues to use the native biome generation settings
exposed by `FlTerraForgedBiomeSource`. Ores and biome placed features remain a
Minecraft-owned stage.

## Known boundary

This remains a hybrid adapter rather than a custom Mojang `DensityFunction`
graph. Vanilla supplies the absolute-Y 3D substrate while FlTerraForged supplies
the final large-scale surface. Newly raised terrain above the original vanilla
surface is therefore solid until later carvers/features act on it; the original
NoiseRouter cave topology is preserved only where it already exists below the
vanilla surface.

This trade-off is intentional for the 1.20.1 reference adapter: it produces a
continuous natural terrain mass without shearing 3D structures. A later
family-level density integration can inject the Engine surface directly into
Minecraft's density graph while keeping the external Engine API unchanged.
