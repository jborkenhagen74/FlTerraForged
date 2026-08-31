# Biome matrix architecture

Biome choice is split into two layers so the upcoming Minecraft version matrix does not freeze FlTerraForged to the 1.20.1 biome set.

## 1. Shared semantic layer

`common/core` contains `BiomeClimateRouter`, `BiomeRole` and `BiomeRoleResolver<T>`. The router consumes only Engine `TerrainSample`/climate semantics and emits a version-neutral role such as `TEMPERATE_FOREST`, `WETLAND`, `ALPINE_MEADOW` or `OCEAN_COLD`. It imports no Minecraft classes.

Climate extremes are separated by intermediate roles. A hot/dry region therefore cannot directly jump to swamp/dense temperate forest merely because a local noise cell changes.

## 2. Minecraft-family mapping

Each Minecraft family implements `BiomeRoleResolver` with the biomes available in that family. The mc1201 implementation uses a data-driven palette encoded as:

```json
"palette": {
  "default": ["minecraft:plains"],
  "temperate_forest": [
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest"
  ],
  "alpine_rock": [
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:frozen_peaks"
  ]
}
```

A later family can list newer native biomes for the same role without changing Engine climate generation or the common router. This is the intended Matrix migration path. Candidate lists may contain more than one biome; the family resolver uses broad climate/terrain signals to choose a stable sub-variant.

## Climate layouts are independent

`climateLayout=randomized` is the normal mode. Seeded climate regions occur in every direction and are smoothly blended. It is also the default for `preset=central_europe`.

`climateLayout=north_south` optionally adds a latitude-like Z gradient. It does not produce hard stripes: regional noise, altitude, continentality, coast moderation and river moisture still perturb the baseline.

Thus these are valid independent combinations:

```text
preset=central_europe + climateLayout=randomized
preset=central_europe + climateLayout=north_south
preset=balanced       + climateLayout=randomized
preset=balanced       + climateLayout=north_south
```

## 1.20.1 mapping

The reference family already uses substantially more than Plains/Desert: cold/frozen/temperate/lukewarm/warm oceans, beach/stony shore, river/frozen river, snowy plains, taiga and old-growth taiga, birch/old-growth birch/forest/flower/dark forest, swamp, meadow, several peak variants, savanna/badlands/jungle families in the general preset. The Central-Europe preset deliberately maps tropical/hot extremes back into temperate candidates. `windswept_hills` is not used as the default hill/mountain mapping.

## Adding a newer Minecraft version

1. Reuse the shared roles/router.
2. Implement the family-native `BiomeRoleResolver`.
3. Populate the family's world-preset palette with every suitable biome added/removed in that Minecraft family.
4. Keep loader-specific registration in Fabric/NeoForge adapters.
5. Run the matrix test for the target family.

This keeps biome availability a **version capability**, not an Engine concern.
