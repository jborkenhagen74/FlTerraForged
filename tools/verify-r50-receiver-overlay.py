#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
generator = (worldgen / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
materializer = (root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard/VanillaBlockMaterializer.java").read_text(encoding="utf-8")
pair = (root / "PAIR-R50-R40.md").read_text(encoding="utf-8")
errors = []

for token in (
        "R50",
        "Engine R40",
        "receiver-owned continuous geometry",
):
    if token not in pair:
        errors.append(f"pair document missing invariant: {token}")

for forbidden in (
        "FinalWetReconciliationPass",
        "HydrologyFillPass",
        "HydrologyCarverGuard",
):
    if forbidden in generator:
        errors.append(f"R50 active generator invokes forbidden repair path: {forbidden}")

for token in (
        "densityBridge.reshape(generated, world)",
        "surfaceGuard.apply(chunk, world)",
        "carver.carve(seed, chunk, carverStep, world)",
        "materializer.decorateWatercourses",
):
    if token not in generator:
        errors.append(f"R50 lifecycle missing inherited stage: {token}")

for token in (
        "MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())",
        "MaterializerHeightQuantizer.exclusiveFluidTop(hydrology.waterSurfaceHeight())",
        "hasContinuousHydrologyWater(sample, hydrology)",
):
    if token not in materializer:
        errors.append(f"R50 lost R49 quantization invariant: {token}")

if errors:
    print("R50 receiver-overlay pair verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R50 lifecycle and Engine R40 pairing verification passed")
