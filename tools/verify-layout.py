#!/usr/bin/env python3
"""Verify FlTerraForged layout and R63 Engine-owned water/performance invariants."""

from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MATRIX = ROOT / "gradle" / "targets.json"
BANNED_ENGINE_API = (
    "net.minecraft.",
    "net.fabricmc.",
    "net.neoforged.",
    "net.minecraftforge.",
    "com.mojang.serialization.",
)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def text(path: Path, label: str) -> str:
    if not path.is_file():
        fail(f"missing {label}: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def require(source: str, tokens, label: str) -> None:
    for token in tokens:
        if token not in source:
            fail(f"{label} missing invariant: {token}")


def forbid(source: str, tokens, label: str) -> None:
    for token in tokens:
        if token in source:
            fail(f"{label} contains forbidden invariant: {token}")


def verify_matrix() -> int:
    data = json.loads(text(MATRIX, "version matrix"))
    targets = data["targets"]
    versions = [target["minecraft"] for target in targets]
    if len(versions) != len(set(versions)):
        fail("duplicate Minecraft target")
    maximum = data.get("snapshotPolicy", {}).get("maxSnapshots", 1)
    snapshots = [target for target in targets if target["channel"] == "snapshot"]
    if len(snapshots) > maximum:
        fail(f"snapshot policy violated: {len(snapshots)} > {maximum}")
    for target in targets:
        family = ROOT / "families" / target["family"]
        for part in ("common", "fabric", "neoforge"):
            if not (family / part).is_dir():
                fail(f"missing family directory: {family / part}")
        for loader in ("fabric", "neoforge"):
            if not (ROOT / "versions" / target["minecraft"] / loader).is_dir():
                fail(f"missing version/loader directory for {target['minecraft']} {loader}")
    return len(targets)


def verify_engine_api() -> None:
    api_root = ROOT / "engine-api/src/main/java"
    for source in api_root.rglob("*.java"):
        source_text = source.read_text(encoding="utf-8")
        for prefix in BANNED_ENGINE_API:
            if prefix in source_text:
                fail(f"forbidden Engine API dependency {prefix!r} in {source.relative_to(ROOT)}")

    version = text(api_root / "dev/foucaultleon/flterraforged/engine/api/EngineApiVersion.java", "Engine API version")
    require(version, ("CURRENT = new EngineApiVersion(0, 2, 0)",), "Engine API")
    terrain_world = text(api_root / "dev/foucaultleon/flterraforged/engine/api/TerrainWorld.java", "TerrainWorld")
    require(
        terrain_world,
        (
            "TerrainSample sample(int x, int z)",
            "default TerrainSample placementSample(int x, int z)",
            "default TerrainSample[] sampleChunk(int chunkX, int chunkZ)",
            "ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ)",
        ),
        "TerrainWorld")
    column = text(
        api_root / "dev/foucaultleon/flterraforged/engine/api/chunk/ColumnSnapshot.java",
        "ColumnSnapshot")
    require(column, ("solidSurfaceY", "waterTopExclusive", "hasSurfaceWater"), "ColumnSnapshot")


def verify_workflow() -> None:
    workflow = text(ROOT / ".github/workflows/build.yml", "build workflow")
    forbid(workflow, ("packages: write", "packages: read", "actions/upload-artifact@v4"), "build workflow")
    require(
        workflow,
        (
            "actions/checkout@v6",
            "actions/setup-java@v6",
            "gradle/actions/setup-gradle@v6",
            "actions/upload-artifact@v7.0.1",
            "--refresh-dependencies",
            "FlTerraForged-R63-Engine-R53-1.20.1-Fabric-${short_sha}.jar",
            "0.1.0-SNAPSHOT-r53",
            "release/r63-water-state-performance",
            "Publish Engine API R63",
        ),
        "R63 workflow")
    if workflow.count("java-version: '21'") < 2 or workflow.count("java-version: '17'") < 2:
        fail("verify and Engine API publication must configure Java 21 plus the Java 17 toolchain")


def verify_engine_owned_worldgen() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    generator = text(worldgen / "FlTerraForgedChunkGenerator.java", "R63 chunk generator")
    for obsolete in (
        "VanillaWorldgenDelegate.java",
        "EngineDensityBridge.java",
        "EngineSurfaceGuard.java",
        "HydrologyCarverGuard.java",
        "HydrologyFillPass.java",
        "TerrainMaterializer.java",
        "VanillaTerrainMaterializer.java",
    ):
        if (worldgen / obsolete).exists():
            fail(f"obsolete hybrid worldgen class still exists: {obsolete}")
    require(
        generator,
        (
            "world.chunkSnapshot(chunkX, chunkZ)",
            "chunkMaterializer.materialize(chunk, snapshot)",
            "CompletableFuture.supplyAsync",
            "Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS)",
            "beginExactBiomePopulation(chunkX, chunkZ, snapshot)",
            "blender.getBiomeSupplier(engineBiomeSource)",
            "MarineStructureGuard.permits",
            "TerrestrialStructureGuard.permits",
            "terrainWorld.placementSample(x, z)",
            "WorldgenTelemetry.Stage.STRUCTURE_PLACEMENT",
            "WorldgenTelemetry.Stage.STRUCTURE_STARTS",
        ),
        "R63 chunk generator")
    forbid(
        generator,
        (
            "NoiseChunkGenerator",
            "VanillaWorldgenDelegate",
            "vanilla.populateNoise",
            "super.populateNoise",
            "super.buildSurface(",
            "super.carve(",
        ),
        "R63 chunk generator")

    biome_source = text(worldgen / "FlTerraForgedBiomeSource.java", "R63 biome source")
    require(
        biome_source,
        (
            "STRUCTURE_SAMPLE_CELL_SIZE = 256",
            "world.placementSample",
            "beginExactBiomePopulation",
            "snapshot.column(localX, localZ).terrain()",
        ),
        "R63 biome source")

    marine = text(worldgen / "MarineEnvironmentCache.java", "R63 marine cache")
    require(marine, ("world.placementSample(x, z)", "ConcurrentHashMap", "SingleFlightCache"), "R63 marine cache")
    forbid(marine, ("world.sample(x, z)", "synchronized"), "R63 marine cache")

    terrestrial = text(worldgen / "TerrestrialStructureGuard.java", "R63 terrestrial structure guard")
    require(
        terrestrial,
        (
            'structureId.startsWith("minecraft:village_")',
            "world.placementSample",
            "MINIMUM_DRY_RATIO = 0.92D",
            "VILLAGE_RADIUS = 48",
        ),
        "R63 terrestrial structure guard")
    forbid(terrestrial, ("world.sample(", "chunkSnapshot("), "R63 terrestrial structure guard")


def verify_materialization() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    materializer = text(worldgen / "EngineChunkMaterializer.java", "R63 Engine chunk materializer")
    require(
        materializer,
        (
            "ChunkSection",
            "currentSection.setBlockState",
            "isSurfaceWater(column, y)",
            "FallbackColumnStates",
            "materializer.fluidState(column.terrain())",
            "case ROCK, DEEP_ROCK -> rock",
            "NaturalMaterialResolver",
        ),
        "R63 Engine chunk materializer")
    forbid(materializer, ("chunk.getBlockState", "NoiseChunkGenerator", "NoiseRouter"), "R63 Engine chunk materializer")

    fallback = text(
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/NaturalMaterialFallback.java",
        "R63 natural material fallback")
    require(
        fallback,
        (
            "canonicalMaterialSample(column)",
            "column.hasSurfaceWater()",
            "waterSurface = column.waterTopExclusive() - 1.0D",
            "case WATER -> materializer.fluidState(sample)",
        ),
        "R63 natural material fallback")


def verify_watercourse_geometry_ownership() -> None:
    decorator = text(
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard/WatercourseDecorator.java",
        "watercourse decorator")
    require(decorator, ("Geometry-neutral post-feature decorator", "Blocks.SEAGRASS", "Blocks.LILY_PAD"), "watercourse decorator")
    forbid(
        decorator,
        ("STAIR_SALT", "DAM_SALT", "SPRAY_SALT", "decorateDams", "placeDam", "Blocks.COBWEB", "OAK_FENCE"),
        "watercourse decorator")


def verify_presets() -> None:
    preset_root = ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset"
    for name in ("flterraforged.json", "central_europe.json", "central_europe_north_south.json"):
        preset = json.loads(text(preset_root / name, "world preset"))
        generator = preset["dimensions"]["minecraft:overworld"]["generator"]
        if generator.get("type") != "flterraforged:chunk_generator":
            fail(f"wrong chunk generator in {name}")
        cool = generator.get("biome_source", {}).get("palette", {}).get("cool_forest", [])
        if "minecraft:forest" not in cool or "minecraft:birch_forest" not in cool:
            fail(f"cool_forest must keep mixed forest plus a limited birch component in {name}")
        if sum("birch" in candidate for candidate in cool) > 1:
            fail(f"cool_forest birch share is too high in {name}")


def main() -> None:
    targets = verify_matrix()
    verify_engine_api()
    verify_workflow()
    verify_engine_owned_worldgen()
    verify_materialization()
    verify_watercourse_geometry_ownership()
    verify_presets()
    print(f"OK: {targets} targets; R63 canonical snapshot water, optimized chunk materialization and cold-start guards verified")


if __name__ == "__main__":
    main()
