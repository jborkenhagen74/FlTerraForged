# FlTerraForged R54

## Baseline and pairing

R54 is built from the promoted R36 host tree and is paired with FlTerraForged-Engine R44. R36 already provides the marine structure environment guard, bounded Minecraft-facing environment caches and the optional variable-height `SurfaceGeometryMaterializer` contract.

R54 extends that provider-aware geometry model into physical river/lake materialization and consumes the receiver-dominant resolved hydrology produced by Engine R44.

## Resolved hydrology materialization

The Engine remains authoritative for semantic terrain, river/lake topology, continuous bed height and resolved water-surface height. The Minecraft host performs only block/provider realization.

`HydrologyFillPass` now samples one immutable provider geometry together with each Engine column:

```text
TerrainWorld.sample(x,z)
        -> MaterializerGeometry.surfaceGeometry(...)
        -> physical wetness decision
        -> exact water/bed realization
```

Hydrology wetness is no longer derived from a full-block-only `solidSurfaceTop` assumption. The resolved Engine water surface must physically lie above `MaterializedSurfaceGeometry.topY()`.

The same physical comparison is used for ocean/coast water. This prevents a partial-height surface from being incorrectly classified as dry merely because its containing Minecraft block cell reaches the same integer Y as the water surface.

## Variable-height provider invariants

A provider may continue to implement only `BlockMaterializer`; existing behavior remains source-compatible.

A provider that emits layered, slab-like or otherwise variable-height terrain should additionally implement `SurfaceGeometryMaterializer` and return its realized continuous `topY`.

R54 also adds the backwards-compatible `BlockMaterializer.submergedHydrologySurfaceState(...)` hook. The default returns the dry bed state unchanged. A provider advertising `MaterializerCapabilities.waterlogging() == true` may override the hook to return the waterlogged form of a partial surface block when Engine water occupies the remaining fraction of that block cell.

No Conquest Reforged block identifiers or classes are referenced by FlTerraForged. Conquest-style support remains a generic provider integration.

## Bed smoothing and provider geometry

The previous integer hydrology-bed smoothing remains enabled for conventional full-block surfaces.

When the active column reports a partial physical surface, R54 treats the provider's containing `blockY` as authoritative and skips integer bed relocation for that column. This prevents host smoothing from moving a variable-height material to a different block layer and invalidating the provider's geometry contract.

## Cascades and waterfalls

Engine R44 guarantees one canonical water level at every drainage node and continuous monotonic segment profiles. R54 materializes the remaining vertical face between neighboring block columns.

When two immediately adjacent, Engine-owned river-core columns differ by at least two integer water levels, the lower core column is filled vertically up to the higher adjacent resolved level. This produces a connected cascade/waterfall sheet instead of an upper watercourse ending in air and a lower watercourse restarting several blocks below.

The extension is deliberately narrow:

- only exact Engine-owned hydrology is considered;
- only `RIVER` terrain is considered;
- both columns must lie in the central half of their resolved river channel;
- lake, coast, ocean and generic dry-gap repair are not promoted into vertical waterfalls;
- one-block normal descent keeps the existing stepped/flowing-water behavior.

## Gap repair

Inferred dry-gap repair remains conservative and bounded. It still requires opposing wet evidence, sufficient wet neighbors and at most one integer level of disagreement. R54 therefore does not use gap repair to bridge real cascades or unrelated water bodies.

## Dependency and caching rules

R54 preserves the R36/R32 deadlock-prevention direction:

```text
Engine immutable sample/cache
        -> provider geometry
        -> hydrology/materialization
        -> marine environment cache / structures
```

Hydrology materialization does not request neighboring Minecraft chunks and does not submit asynchronous generation work. Neighbor evidence is obtained from the already sampled bounded X/Z envelope and therefore reuses the Engine's immutable final-sample cache.

Engine R44 independently replaces river-map hash stripes with exact-key synchronous single flight, so concurrent callers for the same drainage region reuse one generated `Rivermap`.

## Required regression scenarios

R54/R44 must be validated against at least:

1. two rivers joining at different elevations without an air discontinuity;
2. a 2-3 block cascade;
3. a 4+ block waterfall;
4. a lowland river entering coast/ocean without pulling the receiver water column downward;
5. isolated ponds and small water patches without aggressive river-like incision;
6. negative-coordinate region/chunk boundaries;
7. concurrent generation of the same and neighboring regions;
8. a synthetic variable-height provider reporting fractional `topY` values;
9. a synthetic waterlogging provider returning a submerged partial-surface state;
10. all R36 marine-structure location checks.
