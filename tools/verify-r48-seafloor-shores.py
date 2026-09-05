#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
standard = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard"
carver = (worldgen / "FlTerraForgedCarver.java").read_text(encoding="utf-8")
shore = (standard / "ShorelineDecorator.java").read_text(encoding="utf-8")
errors = []

for token in (
        "OCEAN_FLOOR_ROOF = 7",
        "LAKE_FLOOR_ROOF = 6",
        "RIVER_FLOOR_ROOF = 3",
        "buildSurfaceEnvelope",
        "applyLateralSurfaceWaterSeal",
        "lateralWaterLimit",
        "protectedSurfaceWater",
        "surfaceRoofThickness",
        "SurfaceEnvelope",
        "MaterializerGeometry.surfaceGeometry",
        "seedSideWaterContacts",
        "materializer.permitsFinalWetFlow",
        "materializer.finalWetState",
):
    if token not in carver:
        errors.append(f"FlTerraForgedCarver missing R48 invariant: {token}")

if "return 0;" in carver[carver.find("surfaceRoofThickness"):carver.find("buildMask")]:
    errors.append("wet surface roof must never collapse to zero in R48")

for forbidden in (
        "CompletableFuture",
        ".join()",
        "synchronized (",
        "vanilla.carve(",
):
    if forbidden in carver:
        errors.append(f"FlTerraForgedCarver contains forbidden blocking/delegation path: {forbidden}")

for forbidden in (
        "COBBLESTONE_WALL",
        "MOSSY_COBBLESTONE_WALL",
        "pruneSoftVegetation",
):
    if forbidden in shore:
        errors.append(f"ShorelineDecorator retains unsuitable R47 shoreline artifact: {forbidden}")

for token in (
        "MOSSY_COBBLESTONE_SLAB",
        "ANDESITE_SLAB",
        "COBBLESTONE_SLAB",
        "MOSS_CARPET",
        "SUGAR_CANE",
        "FERN",
        "AZALEA",
        "FLOWERING_AZALEA",
        "GROUNDCOVER_SALT",
        "REED_SALT",
        "style < 0.08D",
        "spacing = lushness > 0.68D ? 2 : 3",
):
    if token not in shore:
        errors.append(f"ShorelineDecorator missing R48 lush-shore invariant: {token}")

if errors:
    print("R48 seafloor/lush-shore verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R48 protected submerged floors and lush natural shoreline verification passed")
