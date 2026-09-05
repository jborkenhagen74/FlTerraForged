#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
standard = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard"
carver = (worldgen / "FlTerraForgedCarver.java").read_text(encoding="utf-8")
materializer = (standard / "VanillaBlockMaterializer.java").read_text(encoding="utf-8")
shore_path = standard / "ShorelineDecorator.java"
errors = []

for token in (
        "MaterializerGeometry.surfaceGeometry",
        "surfaceRoofThickness",
        "seedSideWaterContacts",
        "seedFromWetNeighbor",
        "WATER_PRIORITY_OCEAN",
        "WATER_PRIORITY_LAKE",
        "WATER_PRIORITY_RIVER",
        "materializer.permitsFinalWetFlow",
        "materializer.finalWetState",
        "CAVE_ORIGIN_CHANCE = 0.18D",
        "RAVINE_ORIGIN_CHANCE = 0.028D",
):
    if token not in carver:
        errors.append(f"FlTerraForgedCarver missing R47 invariant: {token}")

for forbidden in (
        "CompletableFuture",
        ".join()",
        "synchronized (",
        "vanilla.carve(",
):
    if forbidden in carver:
        errors.append(f"FlTerraForgedCarver contains forbidden blocking/delegation path: {forbidden}")

if "coastUsesLandSurface" in materializer:
    errors.append("VanillaBlockMaterializer must not turn dry coast into noise-driven land surface")
for token in (
        "return coast.choose(sample, x, y, z);",
        "shorelineDecorator.decorate(context);",
):
    if token not in materializer:
        errors.append(f"VanillaBlockMaterializer missing R47 coast/shore invariant: {token}")

if not shore_path.is_file():
    errors.append("missing ShorelineDecorator")
else:
    shore = shore_path.read_text(encoding="utf-8")
    for token in (
            "STYLE_SALT",
            "NaturalMaterialField.sample(x, z, STYLE_SALT, 64.0D)",
            "pruneSoftVegetation",
            "MOSSY_COBBLESTONE_WALL",
            "AZALEA",
            "DEAD_BUSH",
            "RiparianZone.isRiverBank",
            "StandardTerrainTypes.LAKE_SHORE",
            "StandardTerrainTypes.COAST",
    ):
        if token not in shore:
            errors.append(f"ShorelineDecorator missing R47 invariant: {token}")

if errors:
    print("R47 hydraulic shoreline verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R47 hydraulic carver, coast lock and varied shoreline verification passed")
