#!/usr/bin/env python3
"""Verify FlTerraForged matrix, Engine API isolation and R57 Engine-owned worldgen."""

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
            fail(f"{label} contains forbidden legacy invariant: {token}")


def main() -> None:
    data = json.loads(MATRIX.read_text(encoding="utf-8"))
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

    verify_engine_api()
    verify_public_maven_and_workflow()
    verify_mc1201_binding()
    verify_mc1201_engine_owned_worldgen()
    verify_materializer_spi()
    verify_biome_and_presets()
    verify_mc1201_registry_bootstrap(ROOT)
    verify_mc1201_fabric_resource_loader_dependency()

    print(
        f"OK: {len(targets)} targets, {len(snapshots)} snapshot, Engine API 0.2 isolated, "
        "R57 Engine-owned surface/subsurface enforced, mc1201 Fabric binding present"
    )


def verify_engine_api() -> None:
    api_root = ROOT / "engine-api/src/main/java"
    for source in api_root.rglob("*.java"):
        text = source.read_text(encoding="utf-8")
        for prefix in BANNED_ENGINE_API:
            if prefix in text:
                fail(f"forbidden dependency {prefix!r} in {source.relative_to(ROOT)}")

    version = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/EngineApiVersion.java",
        "Engine API version")
    require_tokens(version, ("CURRENT = new EngineApiVersion(0, 2, 0)",), "Engine API 0.2")

    terrain_world = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/TerrainWorld.java",
        "TerrainWorld contract")
    require_tokens(
        terrain_world,
        ("ChunkSnapshot chunkSnapshot(int chunkX, int chunkZ)", "TerrainSample sample(int x, int z)"),
        "TerrainWorld")

    chunk_root = api_root / "dev/foucaultleon/flterraforged/engine/api/chunk"
    chunk_snapshot = require_file(chunk_root / "ChunkSnapshot.java", "ChunkSnapshot API")
    require_tokens(
        chunk_snapshot,
        ("int WIDTH = 16", "ColumnSnapshot column", "NaturalMaterial materialAt", "int minY()", "int maxYExclusive()"),
        "ChunkSnapshot")
    column = require_file(chunk_root / "ColumnSnapshot.java", "ColumnSnapshot API")
    require_tokens(
        column,
        ("TerrainSample terrain", "GeologyType geology", "int solidSurfaceY", "int waterTopExclusive", "int groundwaterY"),
        "ColumnSnapshot")
    natural = require_file(chunk_root / "NaturalMaterial.java", "NaturalMaterial API")
    require_tokens(
        natural,
        ("AIR", "SURFACE", "SOIL", "ROCK", "DEEP_ROCK", "BEDROCK", "WATER", "LAVA"),
        "NaturalMaterial")
    geology = require_file(chunk_root / "GeologyType.java", "GeologyType API")
    require_tokens(
        geology,
        ("SEDIMENTARY", "CARBONATE", "GRANITIC", "METAMORPHIC", "VOLCANIC"),
        "GeologyType")

    river_api = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/river/RiverSample.java",
        "RiverSample API")
    require_tokens(
        river_api,
        ("waterSurfaceHeight", "flow", "public RiverSample(double distance, double width, double depth)"),
        "RiverSample")
    terrain_types = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/terrain/StandardTerrainTypes.java",
        "standard terrain types")
    require_tokens(terrain_types, ("LAKE_SHORE", 'type("lake_shore")'), "standard terrain types")
    capabilities = require_file(
        api_root / "dev/foucaultleon/flterraforged/engine/api/EngineCapability.java",
        "Engine capabilities")
    require_tokens(capabilities, ("RIVER_WATER_LEVEL",), "Engine capabilities")


def verify_public_maven_and_workflow() -> None:
    api_build = require_file(ROOT / "engine-api/build.gradle", "Engine API build")
    workflow = require_file(ROOT / ".github/workflows/build.yml", "build workflow")
    gitignore_lines = set((ROOT / ".gitignore").read_text(encoding="utf-8").splitlines())
    for ignored in ("gradlew", "gradlew.bat", "gradle/wrapper/"):
        if ignored not in gitignore_lines:
            fail(f".gitignore missing required wrapper rule: {ignored}")
    if "maven.pkg.github.com" in api_build:
        fail("engine-api must not publish through GitHub Packages")
    if "build/maven-repository" not in api_build and "maven-repository" not in api_build:
        fail("engine-api build Maven repository is not configured")
    require_tokens(
        workflow,
        ("publish_branch: maven", "release/r57-engine-owned-worldgen", "FlTerraForged-Engine R47"),
        "R57 workflow")
    forbid_tokens(workflow, ("packages: write", "packages: read"), "R57 workflow")
    if workflow.count("java-version: '21'") < 2:
        fail("verify and Engine API publish jobs must run Gradle on Java 21")
    if workflow.count("java-version: '17'") < 2:
        fail("verify and Engine API publish jobs must install Java 17 toolchains")
    if workflow.count("FLTERRAFORGED_JAVA17_HOME=$JAVA_HOME") < 2:
        fail("both jobs must retain the Java 17 toolchain path")
    if workflow.count("org.gradle.java.installations.paths") < 2:
        fail("both Gradle jobs must receive explicit Java 17/21 toolchain paths")
    require_tokens(
        workflow,
        ("actions/upload-artifact@v7.0.1", "archive: false", "FlTerraForged-1.20.1-Fabric-${short_sha}.jar", "if-no-files-found: error"),
        "R57 artifact workflow")


def verify_mc1201_binding() -> None:
    binding_files = (
        ROOT / "versions/1.20.1/fabric/build.gradle",
        ROOT / "versions/1.20.1/fabric/src/main/resources/fabric.mod.json",
        ROOT / "versions/1.20.1/fabric/src/main/resources/flterraforged.mixins.json",
        ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/flterraforged.json",
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/FlTerraForgedChunkGenerator.java",
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/FlTerraForgedBiomeSource.java",
        ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201/mixin/NoiseConfigMixin.java",
    )
    for path in binding_files:
        require_file(path, "Minecraft 1.20.1 Fabric binding file")

    fabric_build = binding_files[0].read_text(encoding="utf-8")
    require_tokens(
        fabric_build,
        ("net.fabricmc.fabric-loom-remap", "flterraforged-engine",
         "exclude group: 'dev.foucaultleon', module: 'flterraforged-engine-api'",
         "implementation project(':engine-api')", "include project(':engine-api')",
         "java.setSrcDirs([", "resources.setSrcDirs([", "JavaLanguageVersion.of(17)", "options.release = 17"),
        "1.20.1 Fabric build")
    forbid_tokens(fabric_build, ("java.srcDirs(", "resources.srcDirs(", "project.version"), "1.20.1 Fabric build")
    require_tokens(fabric_build, ("def modVersion = version.toString()", "expand version: modVersion"), "1.20.1 Fabric build")

    resource_roots = (
        ROOT / "families/mc1201/common/src/main/resources",
        ROOT / "families/mc1201/fabric/src/main/resources",
        ROOT / "platforms/fabric/common/src/main/resources",
        ROOT / "versions/1.20.1/fabric/src/main/resources",
    )
    existing = [path.resolve() for path in resource_roots if path.exists()]
    if len(existing) != len(set(existing)):
        fail("duplicate Minecraft 1.20.1 Fabric resource root")
    descriptors = [file for root in existing for file in root.rglob("fabric.mod.json")]
    if len(descriptors) != 1:
        fail(f"expected exactly one fabric.mod.json across mc1201 resource roots, found {len(descriptors)}")

    relative_resources = {}
    for root in resource_roots:
        if not root.exists():
            continue
        for file in root.rglob("*"):
            if file.is_file():
                relative_resources.setdefault(file.relative_to(root).as_posix(), []).append(file)
    duplicates = {name: files for name, files in relative_resources.items() if len(files) > 1}
    if duplicates:
        fail("duplicate relative resources across mc1201 Fabric source roots")

    session = require_file(
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/EngineWorldSession.java",
        "EngineWorldSession")
    forbid_tokens(session, ("instanceof NoiseConfigSeedAccess",), "EngineWorldSession")
    require_tokens(session, ("(NoiseConfigSeedAccess) (Object) noiseConfig",), "EngineWorldSession")


def verify_mc1201_engine_owned_worldgen() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    generator_path = worldgen / "FlTerraForgedChunkGenerator.java"
    generator = require_file(generator_path, "R57 chunk generator")

    obsolete = (
        "VanillaWorldgenDelegate.java",
        "EngineDensityBridge.java",
        "EngineSurfaceGuard.java",
        "HydrologyCarverGuard.java",
        "HydrologyFillPass.java",
        "TerrainMaterializer.java",
        "VanillaTerrainMaterializer.java",
    )
    for name in obsolete:
        if (worldgen / name).exists():
            fail(f"R57 obsolete hybrid worldgen class still exists: {name}")

    require_tokens(
        generator,
        ("EngineChunkMaterializer", "world.chunkSnapshot(chunkX, chunkZ)",
         "chunkMaterializer.materialize(chunk, snapshot)", "CompletableFuture.supplyAsync",
         "Heightmap.populateHeightmaps(chunk, GENERATED_HEIGHTMAPS)",
         "super.populateBiomes", "super.setStructureStarts", "MarineStructureGuard.permits",
         "super.generateFeatures(world, chunk, structureAccessor);",
         "materializer.decorateWatercourses(new WaterDecorationContext(",
         "super.populateEntities(region);"),
        "R57 chunk generator")
    forbid_tokens(
        generator,
        ("NoiseChunkGenerator", "VanillaWorldgenDelegate", "vanilla.populateNoise", "vanilla.buildSurface",
         "vanilla.carve", "densityBridge", "surfaceGuard", "hydrologyCarverGuard", "hydrologyFillPass",
         "super.buildSurface(", "super.carve("),
        "R57 chunk generator")

    noise_start = generator.find("public CompletableFuture<Chunk> populateNoise(")
    height_start = generator.find("public int getHeight(", noise_start)
    if noise_start < 0 or height_start < 0:
        fail("R57 populateNoise method cannot be located")
    noise = generator[noise_start:height_start]
    require_tokens(
        noise,
        ("world.chunkSnapshot(chunkX, chunkZ)", "chunkMaterializer.materialize(chunk, snapshot)", "return chunk;"),
        "R57 populateNoise")
    forbid_tokens(noise, ("super.populateNoise", "NoiseRouter", "vanilla."), "R57 populateNoise")

    surface_start = generator.find("public void buildSurface(")
    carve_start = generator.find("public void carve(", surface_start)
    entity_start = generator.find("public void populateEntities(", carve_start)
    if min(surface_start, carve_start, entity_start) < 0:
        fail("R57 surface/carver methods cannot be located")
    surface = generator[surface_start:carve_start]
    carve = generator[carve_start:entity_start]
    forbid_tokens(surface, ("super.buildSurface", "setBlockState", "materializer."), "R57 buildSurface")
    forbid_tokens(carve, ("super.carve", "setBlockState", "materializer."), "R57 carve")

    chunk_materializer = require_file(worldgen / "EngineChunkMaterializer.java", "Engine chunk materializer")
    require_tokens(
        chunk_materializer,
        ("ChunkSnapshot", "ColumnSnapshot", "NaturalMaterialResolver", "NaturalMaterialFallback.resolve",
         "resolver.resolveNaturalMaterial", "if (!target.equals(current))", "chunk.setBlockState"),
        "Engine chunk materializer")
    forbid_tokens(
        chunk_materializer,
        ("NoiseChunkGenerator", "NoiseRouter", "Blocks.", "TerrainWorld", "CompletableFuture"),
        "Engine chunk materializer")

    for source in worldgen.glob("*.java"):
        text = source.read_text(encoding="utf-8")
        if "Blocks." in text:
            fail(f"hard-coded block material bypasses materializer layer in {source.relative_to(ROOT)}")
        if "new NoiseChunkGenerator" in text:
            fail(f"R57 worldgen package reintroduces Vanilla natural generation in {source.relative_to(ROOT)}")

    column = require_file(worldgen / "ColumnComposer.java", "lightweight column query composer")
    require_tokens(
        column,
        ("materializer.waterTopExclusive(sample)", "materializer.composedTopState(sample, x, z)",
         "materializer.fillerState(sample, x, surfaceY - 1, z)", "materializer.substrateState(sample)",
         "materializer.fluidState(sample)"),
        "column query composer")
    forbid_tokens(column, ("sample.river().depth() * 0.25",), "column query composer")

    biome_source = require_file(worldgen / "FlTerraForgedBiomeSource.java", "R57 biome source")
    require_tokens(
        biome_source,
        ("beginStructureSampling", "endStructureSampling", "ConcurrentHashMap", "CompletableFuture", "world.sample"),
        "R57 structure biome sampling")


def verify_materializer_spi() -> None:
    api_root = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
    runtime_root = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer"
    standard_root = runtime_root / "standard"

    api_files = (
        "BlockMaterializer.java", "BlockMaterializerProvider.java", "DelegatingBlockMaterializer.java",
        "MaterializerCapabilities.java", "MaterializerContext.java", "MaterializerRegistry.java",
        "WaterDecorationContext.java", "NaturalMaterialResolver.java",
    )
    for name in api_files:
        require_file(api_root / name, "public mc1201 materializer SPI file")

    resolver = require_file(api_root / "NaturalMaterialResolver.java", "natural material resolver SPI")
    require_tokens(
        resolver,
        ("ColumnSnapshot", "NaturalMaterial", "resolveNaturalMaterial"),
        "natural material resolver SPI")

    fallback = require_file(runtime_root / "NaturalMaterialFallback.java", "natural material fallback")
    require_tokens(
        fallback,
        ("case AIR", "case SURFACE", "case SOIL", "case ROCK, DEEP_ROCK", "case BEDROCK", "case WATER", "case LAVA",
         "materializer.composedTopState", "materializer.fillerState", "materializer.substrateState", "Blocks.LAVA"),
        "natural material fallback")

    contract = require_file(api_root / "BlockMaterializer.java", "block materializer contract")
    require_tokens(
        contract,
        ("MaterializerCapabilities capabilities()", "int solidSurfaceY(TerrainSample sample)",
         "int waterTopExclusive(TerrainSample sample)", "BlockState composedTopState(TerrainSample sample, int x, int z)",
         "BlockState fillerState(TerrainSample sample, int x, int y, int z)", "BlockState substrateState(TerrainSample sample)",
         "BlockState bedrockState(TerrainSample sample)", "BlockState fluidState(TerrainSample sample)",
         "default boolean mayRepairHydrologyGap(TerrainSample sample)"),
        "block materializer contract")

    standard = require_file(standard_root / "VanillaBlockMaterializer.java", "standard materializer")
    require_tokens(
        standard,
        ("new MaterializerCapabilities(1.0D, false, false)", "hydrology.hasWaterSurfaceHeight()",
         "hydrology.waterSurfaceHeight()", "hydrology.depth()", "RiparianZone.isDryBank(sample)",
         "StandardTerrainTypes.OCEAN.equals(terrain)", "StandardTerrainTypes.COAST.equals(terrain)"),
        "standard materializer")

    for name in (
        "WatercourseMaterialPalette.java", "MarineMaterialPalette.java", "NaturalMaterialField.java",
        "WatercourseDecorator.java", "VanillaBlockMaterializerProvider.java", "ConfiguredBlockSet.java"):
        require_file(standard_root / name, "standard materializer component")
    require_file(runtime_root / "MaterializerRuntime.java", "materializer runtime")

    fabric_root = ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201/materializer"
    bootstrap = require_file(fabric_root / "FabricMaterializerBootstrap.java", "Fabric materializer bootstrap")
    require_tokens(
        bootstrap,
        ('ENTRYPOINT_KEY = "flterraforged:materializer"',
         "getEntrypoints(ENTRYPOINT_KEY, BlockMaterializerProvider.class)",
         "registry.register(new VanillaBlockMaterializerProvider())",
         "MaterializerRuntime.install(registry, config.materializerId(), config.options())"),
        "Fabric materializer bootstrap")
    config = require_file(fabric_root / "MaterializerConfig.java", "materializer config")
    require_tokens(
        config,
        ('RELATIVE_PATH = "flterraforged/materializer.properties"', 'KEY_MATERIALIZER = "materializer"'),
        "materializer config")
    initializer = require_file(
        ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201/FlTerraForgedFabric.java",
        "Fabric initializer")
    require_tokens(initializer, ("FabricMaterializerBootstrap.bootstrap();",), "Fabric initializer")

    addon = ROOT / "examples/materializer-addon/src/main/resources/fabric.mod.json"
    descriptor = json.loads(require_file(addon, "external materializer add-on example"))
    if "flterraforged:materializer" not in descriptor.get("entrypoints", {}):
        fail("materializer add-on example must use the public Fabric entrypoint")
    require_file(ROOT / "MATERIALIZER-SPI.md", "materializer SPI documentation")
    require_file(ROOT / "WATERCOURSE-MATERIALS.md", "watercourse material documentation")


def verify_biome_and_presets() -> None:
    worldgen = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    router = require_file(worldgen / "NativeBiomeRouter.java", "native biome router")
    require_tokens(router, ("BiomeClimateRouter.route(sample)", "palette.resolve(role, sample)", "seaLevel - sample.surfaceHeight() >= 12.0D"), "native biome router")

    role = require_file(ROOT / "common/src/main/java/dev/foucaultleon/flterraforged/core/biome/BiomeRole.java", "shared biome roles")
    for token in (
        "TEMPERATE_FOREST", "MEDITERRANEAN_WOODLAND", "HOT_DRY", "WETLAND", "ALPINE_ROCK",
        "OCEAN_DEEP_COLD", "OCEAN_DEEP_TEMPERATE", "OCEAN_DEEP_WARM"):
        if token not in role:
            fail(f"shared biome role set is incomplete: {token}")
    climate = require_file(ROOT / "common/src/main/java/dev/foucaultleon/flterraforged/core/biome/BiomeClimateRouter.java", "shared biome climate router")
    require_tokens(
        climate,
        ("HOT_DRY", "HOT_SEASONAL", "MEDITERRANEAN_GRASSLAND", "TEMPERATE_OPEN_WOODLAND", "WETLAND"),
        "shared biome climate router")
    require_file(ROOT / "common/src/main/java/dev/foucaultleon/flterraforged/core/biome/BiomeRoleResolver.java", "shared biome role resolver")

    palette = require_file(worldgen / "BiomePalette.java", "BiomePalette")
    require_tokens(
        palette,
        ("implements BiomeRoleResolver<RegistryEntry<Biome>>", "resolve(BiomeRole role, TerrainSample sample)",
         ".unboundedMap(Codec.STRING", "Biome.REGISTRY_CODEC.listOf()"),
        "BiomePalette")

    normal_tag_path = ROOT / "families/mc1201/common/src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json"
    normal_tag = json.loads(require_file(normal_tag_path, "minecraft:normal world-preset tag"))
    if normal_tag.get("replace", False):
        fail("FlTerraForged must merge with minecraft:normal world presets")
    for preset_id in ("flterraforged:flterraforged", "flterraforged:central_europe", "flterraforged:central_europe_north_south"):
        if preset_id not in normal_tag.get("values", []):
            fail(f"missing world preset from minecraft:normal: {preset_id}")

    preset_paths = (
        ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/flterraforged.json",
        ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/central_europe.json",
        ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/central_europe_north_south.json",
    )
    for path in preset_paths:
        preset = json.loads(require_file(path, "FlTerraForged world preset"))
        overworld = preset["dimensions"]["minecraft:overworld"]["generator"]
        if overworld.get("type") != "flterraforged:chunk_generator":
            fail(f"preset does not use FlTerraForged chunk generator: {path.relative_to(ROOT)}")
        if overworld.get("biome_source", {}).get("type") != "flterraforged:biome_source":
            fail(f"preset does not use FlTerraForged biome source: {path.relative_to(ROOT)}")
        role_map = overworld["biome_source"].get("palette")
        if not isinstance(role_map, dict) or not isinstance(role_map.get("default"), list):
            fail(f"biome palette must be role -> candidate-list: {path.relative_to(ROOT)}")

    central = json.loads(preset_paths[1].read_text(encoding="utf-8"))["dimensions"]["minecraft:overworld"]["generator"]["engine_config"]
    north_south = json.loads(preset_paths[2].read_text(encoding="utf-8"))["dimensions"]["minecraft:overworld"]["generator"]["engine_config"]
    if central.get("preset") != "central_europe" or central.get("climateLayout") != "randomized":
        fail("Central Europe preset must use randomized climate layout")
    if north_south.get("preset") != "central_europe" or north_south.get("climateLayout") != "north_south":
        fail("Central Europe north-south preset must explicitly select north_south")

    for locale in ("en_us", "de_de"):
        language = json.loads(require_file(
            ROOT / f"families/mc1201/common/src/main/resources/assets/flterraforged/lang/{locale}.json",
            f"{locale} translations"))
        for key in (
            "generator.flterraforged.flterraforged",
            "generator.flterraforged.central_europe",
            "generator.flterraforged.central_europe_north_south"):
            if key not in language:
                fail(f"missing {locale} translation for {key}")


def verify_mc1201_registry_bootstrap(root: Path) -> None:
    fabric = root / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201"
    initializer = require_file(fabric / "FlTerraForgedFabric.java", "Fabric initializer")
    registries = require_file(fabric / "FlTerraForgedWorldgenRegistries.java", "worldgen registries")
    biome_mixin = require_file(fabric / "mixin/BiomeSourcesMixin.java", "biome source bootstrap mixin")
    chunk_mixin = require_file(fabric / "mixin/ChunkGeneratorsMixin.java", "chunk generator bootstrap mixin")
    mixin_json = require_file(root / "versions/1.20.1/fabric/src/main/resources/flterraforged.mixins.json", "mixin configuration")

    if "FlTerraForgedWorldgenRegistries.register();" in initializer:
        fail("mc1201 must not register frozen worldgen codec registries from ModInitializer")
    for required in ("BiomeSourcesMixin", "ChunkGeneratorsMixin", "NoiseConfigMixin"):
        if required not in mixin_json:
            fail(f"mc1201 mixin config missing {required}")
    require_tokens(biome_mixin, ('method = "registerAndGetDefault"', "registerBiomeSource(registry)"), "biome source bootstrap mixin")
    require_tokens(chunk_mixin, ('method = "registerAndGetDefault"', "registerChunkGenerator(registry)"), "chunk generator bootstrap mixin")
    require_tokens(registries, ("Registry.register(registry, BIOME_SOURCE_ID", "Registry.register(registry, CHUNK_GENERATOR_ID"), "worldgen registries")


def verify_mc1201_fabric_resource_loader_dependency() -> None:
    gradle_props = require_file(ROOT / "gradle.properties", "Gradle properties")
    build_gradle = require_file(ROOT / "versions/1.20.1/fabric/build.gradle", "mc1201 Fabric build")
    fabric_mod = json.loads(require_file(ROOT / "versions/1.20.1/fabric/src/main/resources/fabric.mod.json", "Fabric mod descriptor"))
    require_tokens(gradle_props, ("mc1201_fabric_api_version=0.92.2+1.20.1",), "Gradle properties")
    require_tokens(build_gradle, ("net.fabricmc.fabric-api:fabric-api",), "mc1201 Fabric build")
    if "fabric-api" not in fabric_mod.get("depends", {}):
        fail("fabric.mod.json does not declare the Fabric API runtime dependency")


if __name__ == "__main__":
    main()
