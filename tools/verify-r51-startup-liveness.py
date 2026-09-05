#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
worldgen = root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
generator = (worldgen / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
marine_cache = (worldgen / "MarineEnvironmentCache.java").read_text(encoding="utf-8")
single_flight = (worldgen / "WorldgenSingleFlightCache.java").read_text(encoding="utf-8")
materializer = (root / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard/VanillaBlockMaterializer.java").read_text(encoding="utf-8")
pair = (root / "PAIR-R51-R41.md").read_text(encoding="utf-8")
errors = []

for token in (
        "FlTerraForged: R51",
        "FlTerraForged Engine: R41",
        "startup-liveness",
        "single-flight",
):
    if token not in pair:
        errors.append(f"R51/R41 pair document missing invariant: {token}")

# STRUCTURE_STARTS must bind only the Engine session. Binding the complete generator here would bind
# the Engine-backed BiomeSource while vanilla is still discovering structure starts and recreates the
# previously isolated 0%-startup recursion path.
if "placementWorld = session.bind(placementCalculator.getNoiseConfig())" not in generator:
    errors.append("structure-start guard no longer uses the Engine-only session bind")
structure_section = generator.split("public CompletableFuture<Chunk> populateBiomes", 1)[0]
if "placementWorld = bind(" in structure_section:
    errors.append("structure-start guard calls the full generator bind and may re-enter BiomeSource")

for forbidden in (
        "FinalWetReconciliationPass",
        "HydrologyFillPass",
        "HydrologyCarverGuard",
):
    if forbidden in generator:
        errors.append(f"R51 active generator invokes forbidden post-generation repair path: {forbidden}")

for token in (
        "WorldgenSingleFlightCache<ColumnKey, MarineColumn>",
        "WorldgenSingleFlightCache<RingKey, RingStats>",
        "ring -> column -> TerrainWorld.environment",
):
    if token not in marine_cache:
        errors.append(f"marine environment cache missing R51 invariant: {token}")
if "OptimisticCache" in marine_cache:
    errors.append("optimistic duplicate marine-cache misses must not return")

for token in (
        "inFlight.putIfAbsent(key, mine)",
        "Recursive worldgen single-flight load for key",
):
    if token not in single_flight:
        errors.append(f"worldgen single-flight cache missing invariant: {token}")
for forbidden in ("supplyAsync", "runAsync", "computeIfAbsent"):
    if forbidden in single_flight:
        errors.append(f"worldgen single-flight loader must not use {forbidden}")

for token in (
        "MaterializerHeightQuantizer.floorBlock(sample.surfaceHeight())",
        "MaterializerHeightQuantizer.exclusiveFluidTop(hydrology.waterSurfaceHeight())",
        "hasContinuousHydrologyWater(sample, hydrology)",
):
    if token not in materializer:
        errors.append(f"R51 lost inherited hydraulic quantization invariant: {token}")

if errors:
    print("R51 startup-liveness verification failed:", file=sys.stderr)
    for error in errors:
        print(f" - {error}", file=sys.stderr)
    raise SystemExit(1)

print("R51 startup-liveness, Engine-only structure binding and marine single-flight verification passed")
