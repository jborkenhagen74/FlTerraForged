# FlTerraForged R53

R53 pairs with Engine R43 to close the remaining narrow-water transition cases: river mouths that run below sea level, narrow outlets between lakes and marine structure candidates that are wet but not actually surrounded by open sea.

## Base

- Parent revision: R52 (`revision/r52-resolved-water-field`)
- Runtime pair: FlTerraForged Engine R43
- R52 provider-resolved variable-height geometry is retained.
- R51 bounded inline single-flight and startup-liveness constraints are retained.
- There is still no Minecraft-side second hydraulic solution or post-generation water repair pass.

## Provider-resolved water-body classification

R53 introduces a Minecraft-side `WaterBodyKind` that is deliberately based on both Engine semantics and the selected block provider's physical geometry:

- `DRY`
- `RIVER`
- `LAKE`
- `CONFINED_CHANNEL`
- `OPEN_MARINE`

`CONFINED_CHANNEL` covers the important failure mode where a center column has marine/ocean semantics but the materialized surrounding water is still a narrow corridor. This can occur at river mouths below sea level or in small connected depressions. It is not sufficient for a structure start that one column is wet and labelled ocean.

The classification is generic. No Conquest Reforged block IDs or shape assumptions are embedded in the host.

## Variable-height materializers

Every water-body probe resolves its center and neighbors through the same R52 geometry path:

1. `TerrainWorld.environment(x, z)` supplies continuous Engine terrain/hydrology semantics.
2. `MaterializerGeometry.surfaceGeometry(...)` resolves the provider's actual X/Z top occupancy.
3. `MaterializerGeometry.hasMaterializableWater(...)` checks whether the water envelope can physically coexist with that provider geometry.
4. Only the resulting materialized wet/dry decision participates in open-water classification.

A non-waterloggable partial-height top is therefore not counted as open water merely because a fluid plane intersects the same integer block cell. Providers with fractional/layered geometry remain authoritative through `SurfaceGeometryMaterializer`, `verticalResolution`, `partialBlocks` and `waterlogging` capabilities.

## Multi-stage marine cache

`MarineEnvironmentCache` now has three bounded synchronous single-flight stages:

1. **Column cache** — 8192 provider-resolved materialized X/Z columns.
2. **Open-water profile cache** — 4096 compact near/far morphology summaries.
3. **Structure ring cache** — 4096 larger 32/64-block rule summaries.

The dependency graph is strictly one-way:

`ring/profile -> column -> TerrainWorld.environment`

No loader schedules work on a Minecraft worldgen executor. Dry/negative results remain cacheable, so repeated rejection of the same confined location does not repeat Engine or provider work.

## Open-water profile

A marine candidate is sampled with a small deterministic stencil before the larger structure rings:

- eight points at 8 blocks: four cardinal plus four diagonal;
- four cardinal points at 16 blocks.

For an underwater center to become `OPEN_MARINE`, R53 requires:

- no inland-water contamination in either stencil;
- at least five material marine samples in the near stencil;
- at least two ocean-semantic samples in the far stencil;
- marine water spanning both sides of at least one near axis.

A narrow channel typically has water only along its longitudinal axis and therefore fails the near-area requirement even if every centerline column is temporarily ocean-classified.

The existing 32/64-block structure rings remain in place after this profile. The new stage is not a replacement for structure-scale depth/open-water checks; it closes the local confinement hole before those checks run.

## Marine structures

Underwater shipwrecks, ocean ruins, monuments and ocean ruined portals must now pass all of the following stages:

1. existing center semantic/material-depth fast rejection;
2. provider-resolved `OPEN_MARINE` profile;
3. existing structure-specific 32-block ring;
4. monument-only 64-block ring where applicable.

Beached shipwrecks keep their dry coast center requirement but additionally need a clean provider-resolved route to open marine water before the larger coastal ring is accepted.

This prevents starts in:

- rivers;
- lakes;
- narrow lake outlets;
- narrow river mouths that happen to lie below sea level;
- isolated or undersized wet depressions;
- physically dry partial-block cells that cannot represent the Engine water plane.

## Hydraulic transition responsibility

R53 does not carve or flatten river mouths itself. Engine R43 changes receiver ownership so an active narrow river remains authoritative through the marginal coast band and a shore-only lake profile does not take over a material outlet prematurely. Open ocean or material lake ownership still takes over once the respective receiver is actually established.

That division keeps hydraulic geometry in the Engine and Minecraft/provider geometry in FlTerraForged.

## Regression targets

- H05/H07/H08/H09: shipwrecks and underwater structures require genuine open marine surroundings.
- River mouth below sea level: no marine structure qualification while the water remains confined.
- Lake-to-lake outlet: no marine qualification and no premature lake-shore receiver cut.
- Variable-height provider: open-water checks use provider top geometry and same-cell waterlogging rules.
- J06/L01/L02: identical environment/profile keys are computed once and reused across concurrent callers.
- K01/K02: no executor submission or cache dependency cycle is introduced.
- K06/K09: all new caches remain bounded.
- Negative coordinates continue to use the existing deterministic world-coordinate sampling path.
