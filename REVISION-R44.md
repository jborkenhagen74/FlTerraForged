# FlTerraForged R44 – Coast, hydrology, performance and biome regression fix

R44 is a corrective revision built directly on R43 after real Minecraft 1.20.1 testing exposed three regressions: disconnected sea-level water pockets along coasts, very slow generation during marine structure placement checks, and large birch-only forest regions. The visible cobweb/carpet representation of waterfall spray is also disabled by default.

## Runtime changes

- Keep the R42/R43 lifecycle invariant: there is no early `createStructurePlacementCalculator()` Engine/BiomeSource bind.
- Pair with Engine R35, where `COAST` is a dry shoreline semantic and submerged oceanward terrain is `OCEAN`.
- Preserve explicit `LAKE`, `RIVER` and `lake_shore` semantics before marine classification so inland hydrology cannot be swallowed by the coastal classifier.
- Reduce the common marine placement check from center + 12 surrounding probes to center + four cardinal probes at 32 blocks.
- Only ocean monuments request the additional four-cardinal outer ring at 64 blocks.
- Cache inner/outer ring results independently so an unused outer ring is never computed.
- Continue to use the lightweight Engine `environment()` API and never `TerrainWorld.sample()` from the placement cache loader.
- Require true `OCEAN` semantics for submerged shipwrecks, ocean ruins, monuments and ocean ruined portals.
- Require a dry `COAST` center plus nearby true ocean for beached shipwrecks.
- Remove birch-only macro forest roles from all shipped 1.20.1 presets. Birch forest remains available as a secondary temperate forest variant.
- Supply `decoration.spray=false` when the option is absent, including existing configurations created before R44. Users may explicitly re-enable the legacy cobweb/carpet spray markers.

## Performance target

For ordinary accepted marine structure starts the environment stencil is reduced from 13 total column probes to 5, a 61.5% reduction. Ocean monuments use 9 instead of 13. Rejected starts stop after the center whenever possible.

## Validation gate

R44 must be tested in a fresh Minecraft world before promotion to `develop`. Required checks are world-creation speed, continuous coastline water, river/lake connectivity, absence of birch monocultures and correct marine structure placement.
