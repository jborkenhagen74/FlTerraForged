#!/usr/bin/env python3
"""Static regression invariants for FlTerraForged R53 open-water confinement."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


cache = read(
    "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/"
    "minecraft/mc1201/worldgen/MarineEnvironmentCache.java"
)
guard = read(
    "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/"
    "minecraft/mc1201/worldgen/MarineStructureGuard.java"
)
kind = read(
    "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/"
    "minecraft/mc1201/worldgen/WaterBodyKind.java"
)
geometry = read(
    "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/"
    "api/mc1201/materializer/MaterializerGeometry.java"
)

for marker in [
    "CONFINED_CHANNEL",
    "OPEN_MARINE",
    "RIVER",
    "LAKE",
]:
    if marker not in kind:
        raise SystemExit(f"missing R53 water-body kind: {marker}")

required_cache = [
    "WorldgenSingleFlightCache<ColumnKey, MarineColumn>",
    "WorldgenSingleFlightCache<ProfileKey, OpenWaterProfile>",
    "WorldgenSingleFlightCache<RingKey, RingStats>",
    "NEAR_RADIUS = 8",
    "FAR_RADIUS = 16",
    "nearMarine >= 5",
    "farOcean >= 2",
    "broadX || broadZ",
    "WaterBodyKind.CONFINED_CHANNEL",
    "MaterializerGeometry.surfaceGeometry(materializer, sample, x, z)",
    "MaterializerGeometry.hasMaterializableWater(",
]
for marker in required_cache:
    if marker not in cache:
        raise SystemExit(f"missing R53 open-water/cache invariant: {marker}")

if "CompletableFuture" in cache or "Executor" in cache:
    raise SystemExit("R53 marine cache loaders must remain synchronous and executor-free")

required_guard = [
    "OpenWaterProfile profile = cache.waterBodyProfile",
    "if (!profile.isOpenMarine())",
    "if (!profile.openMarineAccess())",
    'case "minecraft:shipwreck" -> SHIPWRECK',
    'case "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm" -> OCEAN_RUIN',
    'case "minecraft:monument" -> MONUMENT',
]
for marker in required_guard:
    if marker not in guard:
        raise SystemExit(f"missing R53 structure invariant: {marker}")

required_geometry = [
    "SurfaceGeometryMaterializer",
    "supportsSameCellWater",
    "materializer.capabilities().waterlogging()",
]
for marker in required_geometry:
    if marker not in geometry:
        raise SystemExit(f"missing provider-resolved geometry invariant: {marker}")

print("R53 open-water confinement and provider geometry invariants verified")
