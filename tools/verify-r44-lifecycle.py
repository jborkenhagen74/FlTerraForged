#!/usr/bin/env python3
"""Verify the R44 marine placement, coast and biome regression constraints."""

from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
WORLDGEN = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
MATERIALIZER_API = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
FABRIC_MATERIALIZER = ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201/materializer"
PRESETS = ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset"
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
geometry = (MATERIALIZER_API / "MaterializerGeometry.java").read_text(encoding="utf-8")
surface_geometry = (MATERIALIZER_API / "SurfaceGeometryMaterializer.java").read_text(encoding="utf-8")
terrain_world = (API / "TerrainWorld.java").read_text(encoding="utf-8")
materializer_config = (FABRIC_MATERIALIZER / "MaterializerConfig.java").read_text(encoding="utf-8")

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
require(cache, "RingStats innerRing(", "separately cached inner ring")
require(cache, "RingStats outerRing(", "separately cached outer ring")
require(cache, "private static final int[][] INNER_OFFSETS", "bounded inner cardinal stencil")
require(cache, "private static final int[][] OUTER_OFFSETS", "bounded outer cardinal stencil")
forbid(cache, "{-INNER_RADIUS, -INNER_RADIUS}", "diagonal inner probes restored from R43")
forbid(cache, "{INNER_RADIUS, INNER_RADIUS}", "diagonal inner probes restored from R43")
forbid(cache, "CompletableFuture", "blocking single-flight future in placement cache")
forbid(cache, ".join()", "blocking future wait in placement cache")

require(guard, "static boolean requiresEnvironment", "pure marine relevance filter")
require(guard, "if (!rule.requiresOuterRing)", "staged outer-ring evaluation")
require(guard, "!center.ocean()", "underwater starts require true ocean center")
require(guard, "inner.oceanWater() >= 1", "beached wreck requires nearby open ocean")
forbid(guard, "private static final boolean ENABLED = false", "disabled control guard")

require(terrain_world, "default TerrainEnvironmentSample environment(int x, int z)", "Engine API environment probe")
require(
    surface_geometry,
    "default MaterializedSurfaceGeometry surfaceGeometry(\n            TerrainEnvironmentSample sample",
    "variable-height provider lightweight geometry extension",
)
require(geometry, "TerrainEnvironmentSample sample", "materializer lightweight geometry resolver")

require(materializer_config, 'options.putIfAbsent(KEY_DECORATION_SPRAY, "false")', "safe legacy spray default")

for preset_name in ("flterraforged.json", "central_europe.json", "central_europe_north_south.json"):
    data = json.loads((PRESETS / preset_name).read_text(encoding="utf-8"))
    palette = data["dimensions"]["minecraft:overworld"]["generator"]["biome_source"]["palette"]
    cool_forest = palette.get("cool_forest", [])
    if "minecraft:forest" not in cool_forest:
        errors.append(f"{preset_name}: cool_forest must contain minecraft:forest")
    if cool_forest and all("birch" in biome for biome in cool_forest):
        errors.append(f"{preset_name}: cool_forest must not be a birch-only macro role")
    dense = palette.get("temperate_dense_forest", [])
    if dense and all("birch" in biome for biome in dense):
        errors.append(f"{preset_name}: temperate_dense_forest must not be birch-only")

if errors:
    print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
    raise SystemExit(1)

print("R44 verified: no early bind, staged marine probes, safe spray default, no birch-only macro forest roles")
