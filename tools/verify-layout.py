#!/usr/bin/env python3
"""Verify FlTerraForged layout and R61 Engine-owned worldgen invariants."""

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


def require_file(path: Path, label: str) -> str:
    if not path.is_file():
        fail(f"missing {label}: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def require_tokens(text: str, tokens, label: str) -> None:
    for token in tokens:
        if token not in text:
            fail(f"{label} missing invariant: {token}")


def forbid_tokens(text: str, tokens, label: str) -> None:
    for token in tokens:
        if token in text:
            fail(f"{label} contains forbidden invariant: {token}")


def verify_matrix() -> int:
    data = json.loads(require_file(MATRIX, "version matrix"))
    targets = data["targets"]
    versions = [target["minecraft"] for target in targets]
    if len(versions) != len(set(versions)):
        fail("duplicate Minecraft target")
    snapshots = [target for target in targets if target["channel"] == "snapshot"]
    maximum = data.get("snapshotPolicy", {}).get("maxSnapshots", 1)
    if len(snapshots) > maximum:
        fail(f"snapshot policy violated: {len(snapshots)} > {maximum}")
    for target in targets:
        family = ROOT / "families" / target["family"]
        for part in ("common", "fabric", "neoforge"):
            if not (family / part).is_dir():
                fail(f"missing family directory: {family / part}")
        for loader in ("fabric", "neoforge"):
            directory = ROOT / "versions" / target["minecraft"] / loader
            if not directory.is_dir():
                fail(f"missing version directory: {directory}")
    return len(targets)


def verify_engine_api() -> None:
    api_root = ROOT / "engine-api/src/main/java"
    for source in api_root.rglob("*.java"):
        text = source.read_text(encoding="utf-8")
        for prefix in BANNED_ENGINE_API:
            if prefix in text:
                fail(f"forbidden Engine API dependency {prefix!r} in {source.relative_to(ROOT)}")

    version = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/EngineApiVersion.java",
        "Engine API version")
    require_tokens(version, ("CURRENT = new EngineApiVersion(0, 2, 0)",), "Engine API")

    terrain_world = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/TerrainWorld.java",
        "TerrainWorld")
    require_tokens(
        terrain_world,
        (
            "TerrainSample sample(int x, int z)",
            "default TerrainSample placementSample(int x, int z)",
            "default TerrainSample[] sampleChunk(int chunkX, int chunkZ)",
            "ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ)",
        ),
        "TerrainWorld")

    chunk_root = api_root / "dev/foucaultleon/flterraforged/engine/api/chunk"
    chunk = require_file(chunk_root / "ChunkSnapshot.java", "ChunkSnapshot")
    require_tokens(chunk, ("int WIDTH = 16", "ColumnSnapshot column", "NaturalMaterial materialAt"), "ChunkSnapshot")
    column = require_file(chunk_root / "ColumnSnapshot.java", "ColumnSnapshot")
    require_tokens(column, ("TerrainSample terrain", "GeologyType geology", "waterTopExclusive"), "ColumnSnapshot")
    natural = require_file(chunk_root / "NaturalMaterial.java", "NaturalMaterial")
    require_tokens(natural, ("AIR", "SURFACE", "SOIL", "ROCK", "DEEP_ROCK", "BEDROCK", "WATER", "LAVA"), "NaturalMaterial")


def verify_workflow() -> None:
    workflow = require_file(ROOT / ".github/workflows/build.yml", "build workflow")
    forbid_tokens(workflow, ("packages: write", "packages: read", "actions/upload-artifact@v4"), "build workflow")
    require_tokens(
        workflow,
        (
            "actions/checkout@v6",
            "actions/setup-java@v6",
            "gradle/actions/setup-gradle@v6",
            "actions/upload-artifact@v7.0.1",
            "--refresh-dependencies",
            "publish_branch: maven",
            "FlTerraForged-R61-Engine-R51-1.20.1-Fabric-${short_sha}.jar",
            "0.1.0-SNAPSHOT-r51",
        ),
        "build workflow")
    if workflow.count("java-version: '21'") < 2:
        fail("verify and Engine API publication must use Java 21 build runtime")
    if workflow.count("java-version: '17'") < 2:
        fail("verify and Engine API publication must install Java 17 toolchain")


def verify_mc1201_binding() -> None:
    fabric_build = require_file(ROOT / "versions/1.20.1/fabric/build.gradle", "1.20.1 Fabric build")
    require_tokens(
        fabric_build,
        (
            "net.fabricmc.fabric-loom-remap",
            "flterraforged-engine",
            "implementation project(':engine-api')",
            "include project(':engine-api')",
            "JavaLanguageVersion.of(17)",
            "options.release = 17",
        ),
        "1.20.1 Fabric build")
    forbid_tokens(fabric_build, ("project.version",), "1.20.1 Fabric build")

    session = require_file(
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/EngineWorldSession.java",
        "EngineWorldSession")
    require_tokens(session, ("(NoiseConfigSeedAccess) (Object) noiseConfig",), "EngineWorldSession")
    forbid_tokens(session, ("instanceof NoiseConfigSeedAccess",), "EngineWorldSession")


def verify_engine_owned_worldgen() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    generator = require_file(worldgen / "FlTerraForgedChunkGenerator.java", "R61 chunk generator")

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

    require_tokens(
        generator,
        (
            "world.chunkSnapshot(chunkX, chunkZ)",
            "chunkMaterializer.materialize(chunk, snapshot)",
            "CompletableFuture.supplyAsync",
            "Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS)",
            "super.populateBiomes",
            "beginBiomePopulation",
            "beginExactBiomePopulation(chunkX, chunkZ, snapshot)",
            "blender.getBiomeSupplier(engineBiomeSource)",
            "noiseConfig.getMultiNoiseSampler()",
            "chunk.populateBiomes(",
            "super.generateFeatures(world, chunk, structureAccessor);",
            "MarineStructureGuard.permits",
            "SpawnHelper.populateEntities",
            "ThreadLocal<Integer> structureSamplingDepth",
            "terrainWorld.placementSample(x, z)",
            "WorldgenTelemetry.Stage.STRUCTURE_PLACEMENT",
            "WorldgenTelemetry.Stage.STRUCTURE_STARTS",
        ),
        "R61 chunk generator")
    forbid_tokens(
        generator,
        (
            "NoiseChunkGenerator",
            "VanillaWorldgenDelegate",
            "vanilla.populateNoise",
            "super.populateNoise",
            "super.buildSurface(",
            "super.carve(",
            "super.populateEntities(",
        ),
        "R61 chunk generator")

    biome_source = require_file(worldgen / "FlTerraForgedBiomeSource.java", "R61 biome source")
    require_tokens(
        biome_source,
        (
            "STRUCTURE_SAMPLE_CELL_SIZE = 256",
            "world.placementSample",
            "beginExactBiomePopulation",
            "ExactBiomeScope",
            "snapshot.column(localX, localZ).terrain()",
            "return world.placementSample(blockX, blockZ)",
            "NativeBiomeRouter.route(",
        ),
        "R61 biome source")

    marine = require_file(worldgen / "MarineEnvironmentCache.java", "R61 marine cache")
    require_tokens(
        marine,
        (
            "world.placementSample(x, z)",
            "ConcurrentHashMap",
            "ConcurrentLinkedQueue",
            "SingleFlightCache",
        ),
        "R61 marine cache")
    forbid_tokens(marine, ("world.sample(x, z)", "synchronized"), "R61 marine cache")

    materializer = require_file(worldgen / "EngineChunkMaterializer.java", "R61 Engine chunk materializer")
    require_tokens(
        materializer,
        (
            "ChunkSection",
            "chunk.getSectionIndex(y)",
            "currentSection.setBlockState",
            "requiresChunkWrite(target)",
            "state.hasBlockEntity()",
            "state.getLuminance() > 0",
        ),
        "R61 Engine chunk materializer")
    forbid_tokens(materializer, ("chunk.getBlockState", "NoiseChunkGenerator", "NoiseRouter"), "R61 Engine chunk materializer")

    telemetry = require_file(worldgen / "WorldgenTelemetry.java", "R61 worldgen telemetry")
    require_tokens(
        telemetry,
        (
            "LongAdder",
            "AtomicLong",
            "STRUCTURE_PLACEMENT",
            "STRUCTURE_STARTS",
            "EXACT_BIOMES",
            "SNAPSHOT",
            "MATERIALIZE",
            "NOISE_TOTAL",
            "FEATURES",
        ),
        "R61 telemetry")


def verify_materializer_spi() -> None:
    api_root = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
    runtime_root = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer"
    for name in (
        "BlockMaterializer.java",
        "BlockMaterializerProvider.java",
        "DelegatingBlockMaterializer.java",
        "MaterializerCapabilities.java",
        "MaterializerContext.java",
        "MaterializerRegistry.java",
        "WaterDecorationContext.java",
        "NaturalMaterialResolver.java",
    ):
        require_file(api_root / name, "materializer SPI")
    fallback = require_file(runtime_root / "NaturalMaterialFallback.java", "natural material fallback")
    require_tokens(fallback, ("case AIR", "case SURFACE", "case SOIL", "case ROCK, DEEP_ROCK", "case WATER", "case LAVA"), "natural material fallback")


def verify_biomes_and_presets() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    router = require_file(worldgen / "NativeBiomeRouter.java", "native biome router")
    require_tokens(
        router,
        (
            "BiomeClimateRouter.route(sample)",
            "applyWoodlandDensity",
            "BiomeClimateRouter.woodlandDensity(sample)",
            "macroVariation",
            "palette.resolve(role, sample, blockX, blockZ, seed)",
        ),
        "native biome router")

    climate = require_file(
        ROOT / "common/src/main/java/dev/foucaultleon/flterraforged/core/biome/BiomeClimateRouter.java",
        "biome climate router")
    require_tokens(climate, ("woodlandDensity", "isDryRiparianBank", "TEMPERATE_OPEN_WOODLAND"), "biome climate router")

    palette = require_file(worldgen / "BiomePalette.java", "BiomePalette")
    require_tokens(
        palette,
        (
            "SPECIES_VARIATION_SCALE = 160",
            "spatialSelector",
            "long roleSeed",
            "blockX",
            "blockZ",
            "seed",
        ),
        "R61 BiomePalette")
    forbid_tokens(palette, ("ecological * 0.62D", "role.ordinal() * 0.17320508075688773D"), "R61 BiomePalette")

    preset_root = ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset"
    preset_paths = (
        preset_root / "flterraforged.json",
        preset_root / "central_europe.json",
        preset_root / "central_europe_north_south.json",
    )
    for path in preset_paths:
        preset = json.loads(require_file(path, "FlTerraForged world preset"))
        generator = preset["dimensions"]["minecraft:overworld"]["generator"]
        if generator.get("type") != "flterraforged:chunk_generator":
            fail(f"wrong chunk generator in {path.relative_to(ROOT)}")
        palette_map = generator.get("biome_source", {}).get("palette", {})
        cool = palette_map.get("cool_forest", [])
        if "minecraft:forest" not in cool or "minecraft:birch_forest" not in cool:
            fail(f"cool_forest must retain a small birch component in {path.relative_to(ROOT)}")
        if sum("birch" in candidate for candidate in cool) > 1:
            fail(f"cool_forest birch share is too high in {path.relative_to(ROOT)}")
        for role in ("temperate_open_woodland", "temperate_forest", "temperate_dense_forest", "mediterranean_woodland"):
            if any("birch" in candidate for candidate in palette_map.get(role, [])):
                fail(f"unexpected birch candidate in {role} of {path.relative_to(ROOT)}")
        open_woodland = palette_map.get("temperate_open_woodland", [])
        if open_woodland.count("minecraft:plains") < 2 or "minecraft:forest" not in open_woodland:
            fail(f"open woodland must contain weighted clearings and forest in {path.relative_to(ROOT)}")

    central = json.loads(preset_paths[1].read_text(encoding="utf-8"))["dimensions"]["minecraft:overworld"]["generator"]["engine_config"]
    north_south = json.loads(preset_paths[2].read_text(encoding="utf-8"))["dimensions"]["minecraft:overworld"]["generator"]["engine_config"]
    if central.get("preset") != "central_europe" or central.get("climateLayout") != "randomized":
        fail("Central Europe preset must retain randomized climate layout")
    if north_south.get("preset") != "central_europe" or north_south.get("climateLayout") != "north_south":
        fail("Central Europe north-south preset must retain north_south climate layout")
    for config in (central, north_south):
        if float(config.get("terrainHillsWeight", "0")) < 0.35:
            fail("Europe presets must remain hill-dominant rather than flat")
        if float(config.get("relief", "0")) < 48.0:
            fail("Europe presets must retain increased relief")


def verify_registry_and_fabric() -> None:
    fabric = ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201"
    initializer = require_file(fabric / "FlTerraForgedFabric.java", "Fabric initializer")
    registries = require_file(fabric / "FlTerraForgedWorldgenRegistries.java", "worldgen registries")
    mixin_json = require_file(ROOT / "versions/1.20.1/fabric/src/main/resources/flterraforged.mixins.json", "mixin config")
    if "FlTerraForgedWorldgenRegistries.register();" in initializer:
        fail("worldgen codec registries must not be registered after bootstrap freeze")
    for required in ("BiomeSourcesMixin", "ChunkGeneratorsMixin", "NoiseConfigMixin"):
        if required not in mixin_json:
            fail(f"mixin config missing {required}")
    require_tokens(registries, ("Registry.register(registry, BIOME_SOURCE_ID", "Registry.register(registry, CHUNK_GENERATOR_ID"), "worldgen registries")

    gradle_props = require_file(ROOT / "gradle.properties", "Gradle properties")
    fabric_mod = json.loads(require_file(ROOT / "versions/1.20.1/fabric/src/main/resources/fabric.mod.json", "Fabric descriptor"))
    require_tokens(gradle_props, ("mc1201_fabric_api_version=0.92.2+1.20.1",), "Gradle properties")
    if "fabric-api" not in fabric_mod.get("depends", {}):
        fail("fabric.mod.json must declare Fabric API")


def main() -> None:
    target_count = verify_matrix()
    verify_engine_api()
    verify_workflow()
    verify_mc1201_binding()
    verify_engine_owned_worldgen()
    verify_materializer_spi()
    verify_biomes_and_presets()
    verify_registry_and_fabric()
    print(
        f"OK: {target_count} targets; R61 cold-start guards, Blender-aware exact biome refinement, "
        "mixed woodland and direct section materialization verified"
    )


if __name__ == "__main__":
    main()
