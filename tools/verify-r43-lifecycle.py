#!/usr/bin/env python3
"""Verify the R43 structure-placement lifecycle isolated by the real Minecraft startup test."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
WORLDGEN = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
MATERIALIZER = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
API = ROOT / "engine-api/src/main/java/dev/foucaultleon/flterraforged/engine/api"
errors = []


def require(text, token, label):
    if token not in text:
        errors.append(f"missing {label}: {token}")


def forbid(text, token, label):
    if token in text:
        errors.append(f"forbidden {label}: {token}")


generator = (WORLDGEN / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
cache = (WORLDGEN / "MarineEnvironmentCache.java").read_text(encoding="utf-8")
guard = (WORLDGEN / "MarineStructureGuard.java").read_text(encoding="utf-8")
geometry = (MATERIALIZER / "MaterializerGeometry.java").read_text(encoding="utf-8")
surface_geometry = (MATERIALIZER / "SurfaceGeometryMaterializer.java").read_text(encoding="utf-8")
terrain_world = (API / "TerrainWorld.java").read_text(encoding="utf-8")

after_codec = generator[generator.find("protected Codec"):]
forbid(
    after_codec,
    "createStructurePlacementCalculator(",
    "early structure-placement override; R42 proved this can bind Engine/BiomeSource too early",
)
require(generator, "public void setStructureStarts(", "post-vanilla structure-start filter")
require(
    generator,
    "placementWorld = session.bind(placementCalculator.getNoiseConfig());",
    "Engine-only placement binding",
)
forbid(
    generator,
    "placementWorld = bind(placementCalculator.getNoiseConfig());",
    "BiomeSource-binding generator bind in STRUCTURE_STARTS",
)
require(
    generator,
    "MarineStructureGuard.requiresEnvironment(structureId, hasChildren)",
    "zero-Engine fast path for irrelevant/empty starts",
)

require(cache, "TerrainEnvironmentSample sample = world.environment(x, z);", "lightweight cache loader")
resolve_start = cache.find("private MarineColumn resolveColumn")
resolve_end = cache.find("private double materializedWaterTop", resolve_start)
resolve_body = cache[resolve_start:resolve_end]
forbid(resolve_body, "world.sample(", "full TerrainWorld sample in placement cache loader")
require(
    resolve_body,
    "MaterializerGeometry.surfaceGeometry(materializer, sample, x, z)",
    "provider-aware physical surface geometry",
)

require(guard, "static boolean requiresEnvironment", "pure marine relevance filter")
forbid(guard, "private static final boolean ENABLED = false", "disabled control guard")
require(terrain_world, "default TerrainEnvironmentSample environment(int x, int z)", "Engine API environment probe")
require(
    surface_geometry,
    "default MaterializedSurfaceGeometry surfaceGeometry(\n            TerrainEnvironmentSample sample",
    "variable-height provider lightweight geometry extension",
)
require(
    geometry,
    "TerrainEnvironmentSample sample",
    "materializer lightweight geometry resolver",
)

if errors:
    print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
    raise SystemExit(1)

print("R43 lifecycle verified: no early placement bind, lightweight environment cache, provider geometry enabled")
