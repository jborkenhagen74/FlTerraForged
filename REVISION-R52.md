# FlTerraForged R52

R52 consumes the Engine R42 resolved hydraulic result without re-solving river/lake/ocean geometry on the Minecraft side and completes the variable-height materializer path required by Conquest-style providers.

## Base

- Parent revision: R51 (`revision/r51-startup-liveness`)
- Runtime pair: FlTerraForged Engine R42
- R51 marine single-flight cache and startup-liveness constraints are retained.
- R49 canonical near-integer height quantization is retained.

## One hydraulic truth

The obsolete `HydrologyFillPass` has been removed. It was no longer part of the active R46+ lifecycle but still contained legacy host-side bed smoothing and inferred gap repair. R52 deliberately keeps no second Minecraft-side hydraulic solution.

The active lifecycle is:

1. Engine R42 resolves terrain, river joins, lake/ocean receiver ownership and final continuous water/bed semantics.
2. FlTerraForged asks the selected materializer for physical X/Z surface geometry.
3. The owned carver consumes that immutable semantic/physical envelope.
4. Native features and provider decorations run afterwards.

There is no post-generation water reconstruction pass.

## Variable-height block providers

R52 treats `SurfaceGeometryMaterializer` as authoritative wherever X/Z-specific physical height matters.

Updated paths:

- `ColumnComposer`
- `ChunkGenerator#getHeight`
- `EngineDensityBridge`
- `EngineSurfaceGuard`
- `MarineEnvironmentCache`
- existing owned-carver surface envelope

`MaterializerGeometry` now centralizes two additional decisions:

- whether the final water envelope can actually be materialized around the provider's physical top;
- the first Y cell that may contain water.

A complete fluid cell above the provider geometry always works. Water that occupies the same Minecraft cell as a partial-height top requires `MaterializerCapabilities.waterlogging() == true` and the provider wet hooks.

A non-waterloggable partial block is never replaced merely because an integer water top overlaps its cell.

## Provider responsibilities

A Conquest-style provider should:

1. advertise its real `verticalResolution`, `partialBlocks` and `waterlogging` capabilities;
2. implement `SurfaceGeometryMaterializer` and return the actual occupied `blockY` and physical `topY` for each X/Z column;
3. return the intended partial/layered surface state from the normal materializer surface hooks;
4. override `permitsFinalWetFlow` and `finalWetState` if its partial states can be waterlogged;
5. remain deterministic for seed/config/X/Z so height queries, chunk filling and structure checks see identical geometry.

No Conquest Reforged block IDs are hardcoded into FlTerraForged.

## Structure checks

Marine structure environment sampling now uses the same canonical near-integer fluid-top quantizer and provider-aware wet-geometry rules as normal generation. A custom partial block therefore cannot make a location appear to contain usable marine water when the provider cannot physically represent that water.

## Regression targets

- River mouths do not retain a lower river trench or water plane inside a lake/ocean receiver.
- Higher-flow main stems own compatible river confluences.
- No one-block dry strips are introduced by inconsistent host quantization.
- X/Z-dependent partial surfaces report consistent `getHeight`, column samples and generated chunks.
- Waterloggable top states remain intact and are waterlogged through provider hooks.
- Non-waterloggable partial top states remain intact.
- Marine structure eligibility reflects the same physical provider geometry.
- R51 startup-liveness and cache single-flight behavior remain unchanged.
