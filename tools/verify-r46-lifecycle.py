#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
generator = (worldgen / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
carver = (worldgen / "FlTerraForgedCarver.java").read_text(encoding="utf-8")
delegate = (worldgen / "VanillaWorldgenDelegate.java").read_text(encoding="utf-8")
errors = []

for token in (
        "private final FlTerraForgedCarver carver;",
        "carver.carve(seed, chunk, carverStep, world);",
        "Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS);",
):
    if token not in generator:
        errors.append(f"generator missing R46 owned-carver invariant: {token}")

for forbidden in (
        "FinalWetReconciliationPass",
        "vanilla.carve(",
        "createStructurePlacementCalculator(",
):
    if forbidden in generator:
        errors.append(f"generator contains forbidden R46 lifecycle path: {forbidden}")

if "void carve(" in delegate:
    errors.append("VanillaWorldgenDelegate must not expose carver delegation in R46")

for token in (
        "boolean[] mask",
        "resolveConnectedWater",
        "SOURCE_RADIUS = 2",
        "HALO = 1",
        "materializer.hasFinalWetEnvelope",
        "materializer.fluidState(sample)",
        "materializer.airState(sample)",
        "step != GenerationStep.Carver.AIR",
):
    if token not in carver:
        errors.append(f"FlTerraForgedCarver missing invariant: {token}")

for forbidden in (
        "CompletableFuture",
        ".join()",
        "synchronized (",
        "HydrologyFillPass",
        "HydrologyCarverGuard",
        "FinalWetReconciliationPass",
):
    if forbidden in carver:
        errors.append(f"FlTerraForgedCarver contains forbidden blocking/repair path: {forbidden}")

if (worldgen / "FinalWetReconciliationPass.java").exists():
    errors.append("R46 must remove the post-carver FinalWetReconciliationPass implementation")

if errors:
    print("R46 lifecycle verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R46 owned-carver lifecycle verification passed")
