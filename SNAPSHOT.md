## r16 – MC 1.20.1 world-preset discovery fix

- Added the actual `data/flterraforged/worldgen/world_preset/flterraforged.json` datapack resource.
- Added `flterraforged:flterraforged` to the `minecraft:normal` world-preset tag without replacing Vanilla presets.
- Added German and English `generator.flterraforged.flterraforged` translations so the selector shows `FlTerraForged`.
- Extended layout verification to require preset, tag, and translations.

# Snapshot

Current repository snapshot: **0.1.0-SNAPSHOT-r30**.

Current package revision: **r30** 0.1.0-SNAPSHOT

Purpose: freeze the first architecture contract before importing upstream
worldgen code.

Acceptance criteria:

- [x] Engine API has no Minecraft/loader dependencies.
- [x] Engine API targets Java 17.
- [x] External engines have a Provider -> Engine -> World lifecycle.
- [x] Terrain samples preserve fractional heights.
- [x] Optional engine data is capability-based.
- [x] Engine discovery exists without binding a concrete engine.
- [x] TerraBlender is represented only as optional compatibility.
- [x] Fabric and NeoForge are both represented in the matrix skeleton.
- [x] Common -> family -> exact version policy is documented and verified.
- [x] Only one snapshot is present in the target matrix.
- [x] No upstream implementation source is imported in this snapshot.

## Build fix

The snapshot explicitly declares the JUnit Platform launcher as a test runtime dependency for Gradle 9.x.


## r3 distribution model

Engine API snapshots are distributed from the repository's public `maven` branch,
not GitHub Packages. The generated repository originates at
`build/maven-repository` and retains Maven's normal version directory structure.

### r7 integration checkpoint

Fabric 1.20.1 is the first compiled target in the matrix. It consumes the public external-engine Maven artifact while substituting the Engine POM's transitive API with the local `:engine-api` project. It embeds Engine/API/common dependencies explicitly, registers data-driven worldgen codecs and ships a selectable world preset.

- CI/Gradle runtime is Java 21; Minecraft 1.20.1 bytecode remains Java 17.


## Minecraft 1.20.1 integration status (r12)

The Fabric reference adapter is functionally connected to vanilla NoiseRouter/aquifers, surface rules, carvers, placed features/ores and mob population. FlTerraForged remains authoritative for surface shape, rivers, climate and biome routing.


## Minecraft 1.20.1 validation gate (r13)

`MC1201-TEST-MATRIX.md` defines the mandatory P0 and extended P1 test gates that must pass before the 1.20.1 Fabric adapter is used as the reference implementation for another Minecraft API family.

## r14 CI artifact

Successful Verify runs publish the installable Minecraft 1.20.1 Fabric JAR directly as a 30-day GitHub Actions artifact and expose its download link/digest in the job summary.


### Minecraft 1.20.1 registry bootstrap

Biome-source and chunk-generator codecs are registered from mixins into the vanilla bootstrap helpers before the built-in registries freeze; the normal Fabric entrypoint only validates the result.


### r18 runtime correction

Minecraft 1.20.1 Fabric now explicitly requires Fabric API 0.92.2+1.20.1 so `fabric-resource-loader-v0` exposes the embedded world-preset data pack to the dynamic worldgen registries.


### r19 fluid-stability correction

Historical r19 mitigation: underground aquifer fluids were temporarily removed
from translated columns to prevent lifted lava and fluid-update cascades. This
intermediate policy is superseded by r20, which removes vertical translation
entirely and therefore keeps vanilla aquifers at absolute world Y.


### r20 terrain-continuity correction

The Minecraft 1.20.1 density bridge no longer applies per-column vertical
translation to the vanilla 3D substrate. Vanilla caves, underground layers and
aquifers remain at absolute Y. Engine terrain truncates or extends the substrate
to the target surface, with a six-block solid pre-carver surface skin. This
removes the floating-platform, horizontal-gap and vertical-shear artefacts seen
in r19 while retaining vanilla carvers, surface rules and features.


### r21 river-water integration

`RiverSample` now carries optional `waterSurfaceHeight` and `flow` values while preserving its legacy
three-argument constructor. The Minecraft 1.20.1 adapter consumes those values through a single
`TerrainMaterializer` rule shared by density shaping and synchronous column/height sampling. River water
is therefore based on the directed drainage segment rather than a per-column depth guess. Ocean-floor
height queries still report the bed; world-surface queries include materialized water. Depression-filled
ponds/lakes are materialized through the same rule; explicit waterfall/rapid shaping remains a follow-up.


### r23 climate-aware hydrology integration

r23 was paired with Engine r18. It keeps depression-aware ponds/lakes and terrain-refined
river centerlines, removes the old forced river-sand surface behavior and keeps water placement routed
through `TerrainMaterializer`. Engine r18 weights local runoff by pre-river moisture/temperature, raises
the effective source threshold and uses a wider drainage grid, so dry catchments produce far fewer
small rivers while large rivers can still cross arid terrain. The 1.20.1 adapter adds a narrow riparian
fringe: dry banks route to plains vegetation and are forced to grass over dirt instead of remaining
desert sand immediately beside persistent water.
### r24 desert balance and hydrology carver protection

r24 narrows the vanilla desert decision to the hottest/driest climate tail and adds a post-carver
hydrology repair pass for the Minecraft 1.20.1 reference binding. Rivers, ponds and lakes keep a
five-block solid bed seal, restored Engine water and a one-block subsurface side-wall seal while
vanilla caves remain enabled elsewhere. Use Engine r21 or newer so mountain river water levels are
already bank-contained before this host materializes them.



### r25 basin lakes and vertical-resolution materialization

r25 pairs with Engine r22. Connected inland-water basins use one flat Engine-owned water level and
export dry shores separately as `LAKE_SHORE`. The Minecraft 1.20.1 host now realizes continuous
terrain through `TerrainMaterializer`; the vanilla implementation has 1.0-block resolution and lowers
only shallow lake beds that would otherwise quantize to the same integer Y as their water surface.
This guarantees a real water block without baking Conquest-specific slabs into the Engine. Future
Conquest materializers can advertise finer vertical resolution and waterlogging support.


### r26 replaceable block-materializer SPI

r26 moves Minecraft block realization behind the public mc1201 `BlockMaterializer` SPI. Fabric discovers add-on providers through `flterraforged:materializer`; the selected namespaced provider id is read from `config/flterraforged/materializer.properties`. The built-in `flterraforged:vanilla` provider remains the default. Density, column, surface and carver-repair stages no longer own concrete vanilla block choices.


### r27 climate, biome matrix and materialization finalization

r27 makes biome realization data-driven per Minecraft family through shared semantic `BiomeRole` values and `BiomeRoleResolver`. It adds Central-Europe randomized and optional north-south presets, configurable standard-materializer block sets, isolated hydrology-gap repair and a wider cave-margin seal. Newer Minecraft families can map the same roles to richer biome candidate lists without changing the Engine.


### r28 marine depth, shoreline and riparian correction

r28 pairs with Engine r26. The mc1201 family distinguishes shallow and deep ocean roles from the Engine bathymetry so native deep-ocean biomes can control underwater decoration more naturally. Dry riverbanks now outrank generic coast/sand routing, restoring vegetated corridors through desert terrain. Submerged coast columns participate in the final marine fill repair. The standard materializer increases its default river/lake cave margin to six blocks and bank-seal depth to eight blocks while keeping both options provider-configurable.


### r29 height-/climate-aware watercourse materialization

r29 pairs with Engine r27. Rivers and lakes use a standard three-zone full-block material system
selected from height and climate profiles, while global and profile-specific blocksets remain
configurable. Position-aware materializer hooks keep block patches spatially coherent. Engine r27
deepens broad lowland rivers and connected lake basins without turning every small pond into a deep
lake. Decorative plants, partial blocks and waterfall spray remain a separate follow-up pass.

### r30 continuous water geometry and version-bound decoration

r30 pairs with Engine r28. Continuous basin distance replaces cell-local lake depth estimates;
the final host pass repairs only proven enclosed ridges. Low-frequency material formations replace
the former fixed patches, outer banks fade back into native biome surfaces, and the mc1201-only
decorator adds validated plants, waterlogged stairs, carpets, spray and rare dams. The build matrix
is intentionally unchanged until the 1.20.1 runtime stability gate passes.
