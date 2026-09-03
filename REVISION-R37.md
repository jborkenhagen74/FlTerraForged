# FlTerraForged R37

## Purpose

R37 is a runtime-liveness correction for R36 after Minecraft 1.20.1 spawn generation could remain at 0% despite green CI. The marine structure rules and variable-height materializer geometry remain intact; the execution strategy is changed so structure checks cannot make Minecraft world-generation workers synchronously wait on one another.

R37 is paired with FlTerraForged Engine R33.

## Marine cache liveness

`MarineEnvironmentCache` no longer uses futures or blocking single-flight ownership. Both completed cache levels remain bounded, but a cold miss now follows an optimistic deterministic pattern:

1. short completed-cache lookup;
2. calculate outside the cache monitor;
3. short second lookup;
4. retain one completed value.

Concurrent races may perform a bounded duplicate calculation. They never retain duplicate state and, more importantly, never create a cache-mediated wait edge between Minecraft chunk workers.

## Sparse Engine sampling

R37 keeps the same center/inner/outer marine stencil, but Engine R33 no longer turns every isolated stencil point into a complete 16x16 terrain tile. Sparse points use a bounded exact-point cache and only dense access promotes a tile to bulk generation.

This specifically addresses the R36 amplification where one 13-column marine summary could touch many separated Engine tiles and trigger complete bulk calculations for each one.

## Beached shipwreck fast path

Beached shipwreck starts now validate their center before requesting any perimeter summary. Inland water, unsupported terrain and otherwise implausible centers are rejected after one cached column. The radius-32/radius-64 stencil is calculated only for plausible coast candidates.

## Provider geometry

The R36 variable-height provider SPI is unchanged. Partial-height materializers such as a Conquest Reforged integration can continue to report their physical surface top through `SurfaceGeometryMaterializer` / `MaterializedSurfaceGeometry`; marine water depth is evaluated against that physical geometry rather than assuming full-height blocks.

## Validation rule

R37/R33 are deliberately kept on revision branches until Minecraft runtime validation proves that a fresh 1.20.1 world advances beyond 0%. Green Gradle/Javadoc CI alone is no longer considered sufficient evidence for worldgen liveness.
