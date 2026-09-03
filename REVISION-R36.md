# FlTerraForged R36

## Baseline and purpose

R36 is intentionally rebuilt from the stable R31 host line and is paired with FlTerraForged Engine R32, which itself preserves the actual Engine R29 terrain semantics. The later experimental marine-guard/cache line is not used as the implementation base.

R36 has two goals:

1. prevent underwater structures from starting on land, in rivers, lakes, lake shores or isolated/shallow water patches;
2. make all resulting environment checks reusable and provider-aware without introducing world-generation dependency cycles.

## Staged marine environment check

The structure guard is deliberately cheap for the common case:

1. Empty structure starts and unrelated structure IDs return without an Engine sample.
2. Marine candidates resolve one cached center column.
3. Inland water, dry land and insufficient physical water depth are rejected immediately.
4. Only plausible marine centers resolve the shared environment summary.
5. The summary samples a bounded inner ring (radius 32) and outer ring (radius 64) and is reused by all marine rules at that start position.

Rivers, lakes and lake shores are explicit negative evidence. Shipwrecks, ocean ruins, ocean monuments and ocean ruined portals use different minimum depths and marine-area thresholds. Beached shipwrecks use a separate coast rule requiring both a dry/supportable shore and nearby marine water.

## Multi-level bounded caching

`MarineEnvironmentCache` adds Minecraft-facing reuse above the Engine cache:

- level 1: up to 8192 materialized X/Z columns;
- level 2: up to 2048 environment summaries;
- concurrent cold misses for the same key use single-flight ownership;
- owners calculate synchronously on the current worker;
- no nested world-generation task is submitted;
- expensive terrain/provider work is never performed while an LRU monitor is held.

The Engine R32 final-sample cache is underneath this layer. Repeated structure rules can therefore reuse both the Engine terrain tile and the materialized environment observations.

## Variable-height block providers

R36 keeps block selection outside the Engine and extends the existing materializer SPI rather than adding Conquest-Reforged-specific block IDs.

Existing providers remain compatible. A provider that emits partial or layered terrain blocks can additionally implement:

```java
SurfaceGeometryMaterializer
```

and return:

```java
new MaterializedSurfaceGeometry(blockY, physicalTopY, supportsDryPlacement)
```

`physicalTopY` is continuous world-space geometry inside the containing block cell. For example, a terrain block at Y=62 whose realized collision/surface geometry ends at 62.625 reports `blockY=62` and `topY=62.625`.

Providers that do not implement the optional interface automatically retain full-block behavior (`topY = blockY + 1`).

The marine guard computes physical water depth from:

```text
materializer.waterTopExclusive(sample) - materializedSurfaceGeometry.topY()
```

instead of assuming every generated surface fills its complete Minecraft block cell. This is the intended integration point for Conquest Reforged or other providers with variable-height terrain materialization.

## Provider invariants

A `SurfaceGeometryMaterializer` implementation must be deterministic and side-effect free. It may use the Engine sample, X/Z position and its configured palette/state selection. It must not load or generate neighboring chunks.

A provider should advertise the matching `MaterializerCapabilities`, especially `verticalResolution`, `partialBlocks` and `waterlogging`, so the host can expose and validate its capabilities independently from the exact block implementation.

## World-generation dependency rule

The following dependency direction is mandatory:

```text
Engine immutable terrain sample
        -> materializer/provider geometry
        -> marine column cache
        -> marine environment summary
        -> structure rule
```

No cache loader may call back into Minecraft chunk, biome or structure generation. This prevents the recursive wait graphs and executor starvation that the rebuilt R36/R32 line is specifically intended to avoid.
