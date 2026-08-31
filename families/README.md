# Minecraft API families

Die Familiengrenzen dieses Snapshots sind provisional und werden durch reale API-Diffs verifiziert.


## Biome capability rule

Families own the mapping from shared `BiomeRole` semantics to version-native biome candidates. Never add a Minecraft-version biome identifier to Engine/common climate code. When a newer Minecraft family exposes more suitable biomes, extend that family's palette/resolver only.
