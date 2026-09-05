# Pair R53 / R43

This revision pair is intentionally split across the Engine boundary.

## FlTerraForged R53

Branch: `revision/r53-open-water-confinement`

Owns Minecraft- and provider-facing decisions:

- provider-resolved physical top geometry;
- material water availability around partial blocks;
- local confined-channel versus open-marine classification;
- structure environment guards;
- bounded column/profile/ring caches.

## FlTerraForged Engine R43

Branch: `revision/r43-water-body-semantics`

Owns continuous hydraulic geometry:

- river ownership through the marginal coastal corridor;
- transfer to ocean only after strong open-ocean thresholds for an active river;
- narrow lake outlet ownership until material lake water is reached;
- sparse bounded single-flight caching for lightweight environment samples.

## Dependency direction

The Engine remains Minecraft-agnostic. It never inspects blocks, block states, voxel shapes, loaders, Conquest Reforged classes or materializer capabilities.

FlTerraForged consumes continuous Engine semantics and then resolves them through the selected `BlockMaterializer` / `SurfaceGeometryMaterializer`. A Conquest-style provider can therefore report fractional top geometry and waterlogging support without requiring any Engine change.

## Runtime order

1. Engine R43 resolves one continuous terrain/hydrology field.
2. R43 keeps confined river/lake outlet ownership until a real receiving water body is reached.
3. R43 exposes lightweight environment samples through a bounded sparse single-flight cache.
4. R53 resolves each requested X/Z sample through provider physical geometry.
5. R53 reuses those columns in a compact 8/16-block open-water profile.
6. Only `OPEN_MARINE` underwater candidates continue into the existing 32/64-block structure rings.
7. Actual chunk materialization continues through the selected provider; no post-generation hydraulic repair runs.

## Performance invariants

- no asynchronous cache loader;
- no synchronous wait on work submitted to the same worldgen executor;
- no heavy generation inside a global cache monitor;
- no duplicate computation for an identical in-flight cache key;
- bounded Engine environment cache;
- bounded host column/profile/ring caches;
- negative/dry/confined results are retained like positive results;
- no chunk or biome access from placement environment cache loaders.

## Primary acceptance cases

- river mouth below sea level remains visually a narrow river/estuary until true open ocean;
- lake-to-lake outlet remains a narrow channel until material lake entry;
- underwater shipwrecks do not start in rivers, lakes, outlets or isolated water spots;
- beached shipwrecks require a real coast connected to open sea;
- ocean ruins and monuments require broad material marine water;
- variable-height providers produce the same classification from their actual physical geometry;
- concurrent generation reuses identical samples instead of regenerating them;
- world creation and continuous generation remain free of the earlier 0% startup/deadlock regression.
