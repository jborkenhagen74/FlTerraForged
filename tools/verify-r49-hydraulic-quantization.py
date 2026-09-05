#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
api = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
standard = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard"
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
quantizer_path = api / "MaterializerHeightQuantizer.java"
geometry = (api / "MaterializerGeometry.java").read_text(encoding="utf-8")
surface_geometry = (api / "SurfaceGeometryMaterializer.java").read_text(encoding="utf-8")
materializer = (standard / "VanillaBlockMaterializer.java").read_text(encoding="utf-8")
carver = (worldgen / "FlTerraForgedCarver.java").read_text(encoding="utf-8")
generator = (worldgen / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
errors = []

if not quantizer_path.is_file():
    errors.append("missing MaterializerHeightQuantizer")
else:
    quantizer = quantizer_path.read_text(encoding="utf-8")
    for token in (
            "INTEGER_EPSILON = 1.0E-6D",
            "Math.rint(value)",
            "snapNearInteger",
            "floorBlock",
            "exclusiveFluidTop",
    ):
        if token not in quantizer:
            errors.append(f"MaterializerHeightQuantizer missing R49 invariant: {token}")

if "MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())" not in geometry:
    errors.append("MaterializerGeometry fallback must use canonical R49 height quantization")
if "MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())" not in surface_geometry:
    errors.append("SurfaceGeometryMaterializer lightweight default must use R49 height quantization")
if "Math.floor(sample.surfaceHeight())" in surface_geometry:
    errors.append("SurfaceGeometryMaterializer lightweight default still directly floors Engine height")

for token in (
        "MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())",
        "MaterializerHeightQuantizer.exclusiveFluidTop(hydrology.waterSurfaceHeight())",
        "surfaceY = Math.min(surfaceY, context.seaLevel() - 1)",
        "hasContinuousHydrologyWater(sample, hydrology)",
        "surfaceY = Math.min(surfaceY, waterTopExclusive - 2)",
):
    if token not in materializer:
        errors.append(f"VanillaBlockMaterializer missing R49 quantization invariant: {token}")

if "Math.floor(hydrology.waterSurfaceHeight())" in materializer:
    errors.append("VanillaBlockMaterializer still directly floors hydrology water height")

for token in (
        "SEMANTIC_WET_DEPTH = 0.05D",
        "boolean materialWet = materializer.hasFinalWetEnvelope(sample, x, z)",
        "boolean semanticWet = expectsSurfaceWater(sample)",
        "boolean wet = materialWet || semanticWet",
        "StandardTerrainTypes.OCEAN.equals(sample.terrainType())",
        "hydrology.waterSurfaceHeight() > sample.surfaceHeight() + SEMANTIC_WET_DEPTH",
):
    if token not in carver:
        errors.append(f"FlTerraForgedCarver missing R49 semantic-wet invariant: {token}")

# Historical repair classes may remain as dead source for compatibility/history, but the active
# lifecycle must never invoke them. R49 fixes the source semantics before/during materialization.
for forbidden in (
        "FinalWetReconciliationPass",
        "HydrologyFillPass",
        "HydrologyCarverGuard",
):
    if forbidden in generator or forbidden in carver:
        errors.append(f"R49 active worldgen lifecycle invokes forbidden repair path: {forbidden}")

if errors:
    print("R49 hydraulic quantization verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R49 stable block quantization and pre-carve semantic-wet protection verification passed")
