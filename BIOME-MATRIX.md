# Biome matrix architecture

Biome choice is split into two layers so the upcoming Minecraft version matrix does not freeze FlTerraForged to the 1.20.1 biome set.

## 1. Shared semantic layer

`common/core` contains `BiomeClimateRouter`, `BiomeRole` and `BiomeRoleResolver<T>`. The router consumes only Engine `TerrainSample`/climate semantics and emits a version-neutral role such as `TEMPERATE_FOREST`, `WETLAND`, `ALPINE_MEADOW`, `OCEAN_COLD` or the depth-refined `OCEAN_DEEP_TEMPERATE`. It imports no Minecraft classes.

Climate extremes are separated by intermediate roles. A hot/dry region therefore cannot directly jump to swamp/dense temperate forest merely because a local noise cell changes.

## 2. Minecraft-family mapping

Each Minecraft family implements `BiomeRoleResolver` with the biomes available in that family. The mc1201 implementation uses a data-driven palette encoded as:

```json
"palette": {
  "default": ["minecraft:plains"],
  "temperate_forest": [
    "minecraft:forest",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:dark_forest",
    "minecraft:birch_forest"
  ],
  "alpine_rock": [
    "minecraft:stony_peaks",
    "minecraft:jagged_peaks",
    "minecraft:frozen_peaks"
  ]
}
```

A later family can list newer native biomes for the same role without changing Engine climate generation or the common router. This is the intended Matrix migration path. Depth refinement remains family-owned: mc1201 upgrades semantic ocean roles to `OCEAN_DEEP_*` when the Engine floor lies at least 12 blocks below sea level, while later families can choose different native mappings without changing the Engine. Candidate lists may contain more than one biome and may repeat an entry to give it additional weight. `BiomeVariantSelector` assigns broad irregular, seed-dependent stands instead of choosing from per-block noise. This lets a family balance mixed and single-species forests without hard-coding any native biome into the shared selector.

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

The reference family already uses substantially more than Plains/Desert: cold/frozen/temperate/lukewarm/warm shallow oceans plus deep cold/frozen/temperate/lukewarm ocean roles, beach/stony shore, river/frozen river, snowy plains, taiga and old-growth taiga, birch/old-growth birch/forest/flower/dark forest, swamp, meadow, several peak variants, savanna/badlands/jungle families in the general preset. The Central-Europe preset deliberately maps tropical/hot extremes back into temperate candidates and weights the mixed `forest` candidate above individual deciduous monocultures. Birch, old-growth birch, spruce and dark-oak stands remain available. `windswept_hills` is not used as the default hill/mountain mapping.

## Adding a newer Minecraft version

1. Reuse the shared roles/router.
2. Implement the family-native `BiomeRoleResolver`.
3. Populate the family's world-preset palette with every suitable biome added/removed in that Minecraft family.
4. Keep loader-specific registration in Fabric/NeoForge adapters.
5. Run the matrix test for the target family.

This keeps biome availability a **version capability**, not an Engine concern.
