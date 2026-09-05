# FlTerraForged R52 / Engine R42

This pair is the reference implementation for resolved hydraulic ownership plus provider-aware physical surface geometry.

## Engine R42 owns

- continuous terrain and hydrology;
- deterministic receiver priority `OCEAN > LAKE > RIVER > DRY`;
- higher-flow main-stem authority at compatible river joins;
- lake/ocean water-surface authority at mouths;
- bounded mouth-bed blending;
- existing R41 bounded inline single-flight caches and startup-liveness guarantees.

## FlTerraForged R52 owns

- Minecraft block materialization only;
- provider-supplied X/Z physical top geometry;
- canonical integer fluid-top quantization;
- waterlogging handoff for partial-height top states;
- owned cave/ravine materialization;
- marine structure checks against the same physical provider geometry.

## Explicitly forbidden

- Minecraft/chunk access from the Engine resolver;
- host-side re-carving of river/lake beds after Engine resolution;
- inferred post-generation water-gap repair;
- asynchronous worldgen work followed by synchronous waits;
- replacing a non-waterloggable provider partial block with a full fluid block merely to satisfy an overlapping water envelope;
- Conquest Reforged-specific block IDs in the generic host or Engine.

## Conquest-style provider contract

Conquest Reforged or another variable-height materializer plugs into the existing provider SPI. It reports its actual surface cell and physical top height through `SurfaceGeometryMaterializer`, advertises partial-block/waterlogging capabilities and optionally supplies a waterlogged state through the final-wet hooks. Engine R42 remains completely unaware of the concrete Minecraft block family.

## Required validation before matrix expansion

1. Fresh-world generation with R52/R42 must pass R51/R41 startup-liveness checks.
2. River-to-lake and river-to-ocean mouths must show one continuous receiving water plane.
3. River confluences must not contain one-column trenches or dry seams.
4. Negative-coordinate seams around `-17/-16/-15/-1/0/1/15/16/17` must remain deterministic.
5. Parallel same-region generation must produce the same result as sequential generation.
6. A test provider with fractional physical top heights must produce consistent chunk, heightmap and marine-structure results.
7. A waterloggable partial provider must preserve its block state while materializing water; a non-waterloggable provider must preserve the partial solid and defer full fluid to the next complete cell.
