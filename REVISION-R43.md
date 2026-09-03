# FlTerraForged R43

R43 rebuilds marine-structure validation on the runtime-proven R42 lifecycle instead of continuing the R32-R39 early-placement path.

## Baseline and paired Engine

- Host baseline: R42 runtime control (`f980925a068c1437042dbe0f881c81a2872a95c5`), which completed Minecraft 1.20.1 world creation and was observed to start faster than historical R31.
- Paired Engine: R34, rebuilt from exact Engine R29 (`6fc06491d212d2ad38ece463e81925393828b984`).
- Engine API contract: 0.1.1, still published under the existing `0.1.0-SNAPSHOT` Maven coordinate during snapshot development.

## Lifecycle rule

R43 does **not** override `createStructurePlacementCalculator()`. In particular, the structure-placement phase must never call the generator's normal `bind(NoiseConfig)` method, because that method also activates the Engine-backed biome source.

`setStructureStarts()` runs vanilla first. Empty start maps, empty starts and unrelated structures return without binding an Engine world. Only a relevant marine candidate lazily calls:

```java
session.bind(placementCalculator.getNoiseConfig())
```

This opens or reuses the seed-bound Engine world but deliberately leaves `FlTerraForgedBiomeSource` unbound. Normal biome/noise generation retains the established `bind(NoiseConfig)` path and activates the biome source at its normal lifecycle stage.

`tools/verify-r43-lifecycle.py` makes this separation a CI invariant.

## Lightweight environment API

`TerrainWorld.environment(x, z)` returns `TerrainEnvironmentSample` containing only:

- continuous solid surface height;
- continuous hydrologic water-surface height, or NaN for a dry column;
- semantic terrain type.

Engine R34 overrides the default API method. Its implementation performs the R29 terrain/erosion/river/lake computation required for physical water classification but skips the final climate projection and the four neighboring post-river height queries used to derive local gradient for a complete `TerrainSample`.

The regular `TerrainWorld.sample()` contract and final terrain generation remain unchanged.

## Marine checks

R43 validates the following structure ids:

- `minecraft:shipwreck`;
- `minecraft:shipwreck_beached`;
- `minecraft:ocean_ruin_cold`;
- `minecraft:ocean_ruin_warm`;
- `minecraft:monument`;
- `minecraft:ruined_portal_ocean`.

The center check rejects river, lake and lake-shore candidates before a larger stencil is requested. Accepted deep-marine centers are validated against an inner 32-block and outer 64-block stencil. The two-level environment cache is bounded and never synchronously waits for another worldgen worker; cache loads execute outside the short completed-map monitor.

## Variable-height block providers

R43 extends `SurfaceGeometryMaterializer` with a lightweight overload accepting `TerrainEnvironmentSample`. A provider that places layered, slab-like or otherwise partial-height terrain can report its real continuous top surface during structure placement without requesting a full terrain sample or neighboring chunks.

Marine water depth is measured from the materializer-reported physical top (`MaterializedSurfaceGeometry.topY`) to the quantized water top. Legacy/full-block providers automatically retain the full-block fallback.

This is the intended integration point for Conquest-Reforged-style providers; FlTerraForged does not hard-code Conquest block ids or depend on Conquest classes.

## Cache and concurrency constraints

- No `CompletableFuture.join()` or worldgen-worker single-flight wait is introduced by R43/R34.
- No heavy computation is performed while holding the marine completed-cache monitor.
- Environment cache sizes remain bounded: 8192 columns and 2048 summaries per generator instance.
- Engine R34 retains the known-running R29 cache semantics for normal final samples.
- Placement probes do not enter `WorldSampleCache` in the default Engine R34 implementation.

## Runtime gate

Do not promote R43/R34 to `develop` solely because CI is green. The release gate is a clean Minecraft 1.20.1 world creation test using the coupled R43 artifact, followed by marine structure validation and parallel exploration checks.
