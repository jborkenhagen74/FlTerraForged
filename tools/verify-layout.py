#!/usr/bin/env python3
"""Verify FlTerraForged matrix and architecture skeleton without Gradle."""

from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MATRIX = ROOT / "gradle" / "targets.json"
BANNED = (
    "net.minecraft.",
    "net.fabricmc.",
    "net.neoforged.",
    "net.minecraftforge.",
    "com.mojang.serialization.",
)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


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

    api_root = ROOT / "engine-api" / "src" / "main" / "java"
    for source in api_root.rglob("*.java"):
        text = source.read_text(encoding="utf-8")
        for prefix in BANNED:
            if prefix in text:
                fail(f"forbidden dependency {prefix!r} in {source.relative_to(ROOT)}")

    api_build = (ROOT / "engine-api" / "build.gradle").read_text(encoding="utf-8")
    workflow = (ROOT / ".github" / "workflows" / "build.yml").read_text(encoding="utf-8")
    if "maven.pkg.github.com" in api_build:
        fail("engine-api must not publish through GitHub Packages")
    if "build/maven-repository" not in api_build and "maven-repository" not in api_build:
        fail("engine-api build Maven repository is not configured")
    if "publish_branch: maven" not in workflow:
        fail("workflow does not publish the API repository to the maven branch")
    if "packages: write" in workflow or "packages: read" in workflow:
        fail("workflow must not require GitHub Packages permissions")

    binding_files = (
        ROOT / "versions/1.20.1/fabric/build.gradle",
        ROOT / "versions/1.20.1/fabric/src/main/resources/fabric.mod.json",
        ROOT / "versions/1.20.1/fabric/src/main/resources/flterraforged.mixins.json",
        ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/flterraforged.json",
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/FlTerraForgedChunkGenerator.java",
        ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/FlTerraForgedBiomeSource.java",
        ROOT / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201/mixin/NoiseConfigMixin.java",
    )
    for binding_file in binding_files:
        if not binding_file.is_file():
            fail(f"missing Minecraft 1.20.1 Fabric binding file: {binding_file.relative_to(ROOT)}")

    fabric_build = binding_files[0].read_text(encoding="utf-8")
    if "net.fabricmc.fabric-loom-remap" not in fabric_build:
        fail("1.20.1 Fabric binding must use the non-deprecated Loom remap plugin id")
    if "flterraforged-engine" not in fabric_build:
        fail("1.20.1 Fabric binding does not consume the external engine artifact")
    if "exclude group: 'dev.foucaultleon', module: 'flterraforged-engine-api'" not in fabric_build:
        fail("1.20.1 Fabric binding must exclude the Engine POM's remote engine-api dependency")
    if "implementation project(':engine-api')" not in fabric_build:
        fail("1.20.1 Fabric binding must compile against the local :engine-api project")
    if "include project(':engine-api')" not in fabric_build:
        fail("1.20.1 Fabric binding must embed the local :engine-api project")

    if "java.setSrcDirs([" not in fabric_build or "resources.setSrcDirs([" not in fabric_build:
        fail("1.20.1 Fabric binding must replace Gradle's default source roots with setSrcDirs(...)")
    if "java.srcDirs(" in fabric_build or "resources.srcDirs(" in fabric_build:
        fail("1.20.1 Fabric binding appends source roots and can duplicate src/main resources")

    resource_roots = (
        ROOT / "families/mc1201/common/src/main/resources",
        ROOT / "families/mc1201/fabric/src/main/resources",
        ROOT / "platforms/fabric/common/src/main/resources",
        ROOT / "versions/1.20.1/fabric/src/main/resources",
    )
    existing_resource_roots = [root.resolve() for root in resource_roots if root.exists()]
    if len(existing_resource_roots) != len(set(existing_resource_roots)):
        fail("duplicate Minecraft 1.20.1 Fabric resource root")
    mod_descriptors = [
        descriptor
        for root in existing_resource_roots
        for descriptor in root.rglob("fabric.mod.json")
    ]
    if len(mod_descriptors) != 1:
        fail(f"expected exactly one fabric.mod.json across mc1201 resource roots, found {len(mod_descriptors)}")

    relative_resources = {}
    for resource_root in resource_roots:
        if not resource_root.exists():
            continue
        for resource_file in resource_root.rglob("*"):
            if not resource_file.is_file():
                continue
            relative_path = resource_file.relative_to(resource_root).as_posix()
            relative_resources.setdefault(relative_path, []).append(resource_file)
    duplicates = {
        relative_path: files
        for relative_path, files in relative_resources.items()
        if len(files) > 1
    }
    if duplicates:
        formatted = "; ".join(
            f"{relative_path}: {', '.join(str(file.relative_to(ROOT)) for file in files)}"
            for relative_path, files in sorted(duplicates.items())
        )
        fail(f"duplicate relative resources across mc1201 Fabric roots: {formatted}")

    engine_session = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen/EngineWorldSession.java"
    engine_session_text = engine_session.read_text(encoding="utf-8")
    if "instanceof NoiseConfigSeedAccess" in engine_session_text:
        fail("mc1201 EngineWorldSession must not use direct instanceof NoiseConfigSeedAccess")
    if "(NoiseConfigSeedAccess) (Object) noiseConfig" not in engine_session_text:
        fail("mc1201 EngineWorldSession must bridge NoiseConfigSeedAccess through Object")

    worldgen_root = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
    functional_files = (
        worldgen_root / "EngineDensityBridge.java",
        worldgen_root / "VanillaWorldgenDelegate.java",
        worldgen_root / "EngineSurfaceGuard.java",
    )
    for functional_file in functional_files:
        if not functional_file.is_file():
            fail(f"missing functional mc1201 worldgen file: {functional_file.relative_to(ROOT)}")

    generator_text = (worldgen_root / "FlTerraForgedChunkGenerator.java").read_text(encoding="utf-8")
    required_generator_fragments = (
        "vanilla.populateNoise",
        "densityBridge.reshape",
        "vanilla.buildSurface",
        "surfaceGuard.apply",
        "vanilla.carve",
        "vanilla.populateEntities",
    )
    for fragment in required_generator_fragments:
        if fragment not in generator_text:
            fail(f"mc1201 generator is missing functional worldgen delegation: {fragment}")
    if "intentionally a follow-up integration step" in generator_text:
        fail("mc1201 generator still contains deferred carver integration")
    if "simple solid/water columns" in generator_text:
        fail("mc1201 generator still describes the obsolete column-only adapter")

    density_text = functional_files[0].read_text(encoding="utf-8")
    if "if (!state.getFluidState().isEmpty())" not in density_text:
        fail("mc1201 density bridge must explicitly handle translated underground fluids")
    if "return Blocks.AIR.getDefaultState();" not in density_text:
        fail("mc1201 safe density bridge must suppress translated aquifer fluids")
    if "sample.river().depth() * 0.25" in density_text:
        fail("mc1201 density bridge must not recreate unstable per-column highland-river water levels")
    if "if (!state.equals(current))" not in density_text:
        fail("mc1201 density bridge must avoid rewriting unchanged block states")

    column_text = (worldgen_root / "ColumnComposer.java").read_text(encoding="utf-8")
    if "sample.river().depth() * 0.25" in column_text:
        fail("mc1201 synchronous column composer must match stable sea-level-only water policy")

    delegate_text = functional_files[1].read_text(encoding="utf-8")
    for fragment in ("new NoiseChunkGenerator", ".populateNoise(", ".buildSurface(", ".carve(", ".populateEntities("):
        if fragment not in delegate_text:
            fail(f"vanilla worldgen delegate is incomplete: {fragment}")

    preset = json.loads(binding_files[3].read_text(encoding="utf-8"))
    overworld = preset["dimensions"]["minecraft:overworld"]["generator"]
    if overworld.get("type") != "flterraforged:chunk_generator":
        fail("1.20.1 world preset does not use the FlTerraForged chunk generator")
    if overworld.get("biome_source", {}).get("type") != "flterraforged:biome_source":
        fail("1.20.1 world preset does not use the FlTerraForged biome source")

    preset_tag_file = ROOT / "families/mc1201/common/src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json"
    if not preset_tag_file.is_file():
        fail("missing minecraft:normal world-preset tag contribution for FlTerraForged")
    preset_tag = json.loads(preset_tag_file.read_text(encoding="utf-8"))
    if preset_tag.get("replace", False):
        fail("FlTerraForged must merge with, not replace, minecraft:normal world presets")
    if "flterraforged:flterraforged" not in preset_tag.get("values", []):
        fail("FlTerraForged world preset is not exposed through minecraft:normal")

    for locale in ("en_us", "de_de"):
        language_file = ROOT / f"families/mc1201/common/src/main/resources/assets/flterraforged/lang/{locale}.json"
        if not language_file.is_file():
            fail(f"missing {locale} world-preset translation")
        language = json.loads(language_file.read_text(encoding="utf-8"))
        if language.get("generator.flterraforged.flterraforged") != "FlTerraForged":
            fail(f"invalid {locale} FlTerraForged world-preset translation")


    # The Loom-backed multi-project must run Gradle on Java 21 while retaining a
    # Java 17 compilation toolchain for Minecraft 1.20.1. The publish job also
    # configures mc1201-fabric, so both jobs need the Java 21 runtime.
    workflow = (ROOT / '.github' / 'workflows' / 'build.yml').read_text(encoding='utf-8')
    if workflow.count("java-version: '21'") < 2:
        fail('Both verify and Engine API publish jobs must run Gradle on Java 21')
    if workflow.count("java-version: '17'") < 2:
        fail('Both verify and publish jobs must install Java 17 as a toolchain')
    if workflow.count('FLTERRAFORGED_JAVA17_HOME=$JAVA_HOME') < 2:
        fail('Both jobs must remember the Java 17 toolchain path before switching to Java 21')
    if workflow.count('org.gradle.java.installations.paths') < 2:
        fail('Both Gradle invocations must receive explicit Java 17/21 toolchain search paths')
    if 'actions/upload-artifact@v7.0.1' not in workflow:
        fail('Verify job must upload the installable Minecraft 1.20.1 Fabric JAR with upload-artifact v7.0.1')
    if 'archive: false' not in workflow:
        fail('Minecraft test artifact must be uploaded as a direct JAR, not wrapped in an artifact ZIP')
    if 'FlTerraForged-1.20.1-Fabric-${short_sha}.jar' not in workflow:
        fail('Minecraft test artifact must have a deterministic commit-qualified JAR name')
    if workflow.count('if-no-files-found: error') < 1:
        fail('Minecraft artifact upload must fail when the installable JAR is missing')
    if workflow.find("java-version: '17'") > workflow.find("java-version: '21'"):
        fail('Verify job must install Java 17 before activating Java 21 as JAVA_HOME')
    mc1201_build = (ROOT / 'versions' / '1.20.1' / 'fabric' / 'build.gradle').read_text(encoding='utf-8')
    if 'JavaLanguageVersion.of(17)' not in mc1201_build or 'options.release = 17' not in mc1201_build:
        fail('Minecraft 1.20.1 must continue targeting Java 17')
    if 'project.version' in mc1201_build:
        fail('mc1201 build must not access Task.project during processResources execution')
    if 'def modVersion = version.toString()' not in mc1201_build:
        fail('mc1201 build must capture the mod version during configuration')
    if "expand version: modVersion" not in mc1201_build:
        fail('mc1201 processResources must expand the captured modVersion value')

    verify_mc1201_registry_bootstrap(ROOT)
    verify_mc1201_fabric_resource_loader_dependency()

    print(
            f"OK: {len(targets)} targets, {len(snapshots)} snapshot, engine-api isolated, "
            "public Maven publishing configured, mc1201 Fabric functional binding present"
        )



def verify_mc1201_registry_bootstrap(root):
    fabric = root / "families/mc1201/fabric/src/main/java/dev/foucaultleon/flterraforged/fabric/mc1201"
    initializer = (fabric / "FlTerraForgedFabric.java").read_text()
    registries = (fabric / "FlTerraForgedWorldgenRegistries.java").read_text()
    biome_mixin = (fabric / "mixin/BiomeSourcesMixin.java").read_text()
    chunk_mixin = (fabric / "mixin/ChunkGeneratorsMixin.java").read_text()
    mixin_json = (root / "versions/1.20.1/fabric/src/main/resources/flterraforged.mixins.json").read_text()

    if "FlTerraForgedWorldgenRegistries.register();" in initializer:
        raise SystemExit("mc1201 must not register frozen worldgen codec registries from ModInitializer")
    for required in ("BiomeSourcesMixin", "ChunkGeneratorsMixin", "NoiseConfigMixin"):
        if required not in mixin_json:
            raise SystemExit(f"mc1201 mixin config missing {required}")
    if 'method = "registerAndGetDefault"' not in biome_mixin or "registerBiomeSource(registry)" not in biome_mixin:
        raise SystemExit("mc1201 biome-source codec is not registered during vanilla bootstrap")
    if 'method = "registerAndGetDefault"' not in chunk_mixin or "registerChunkGenerator(registry)" not in chunk_mixin:
        raise SystemExit("mc1201 chunk-generator codec is not registered during vanilla bootstrap")
    if "Registry.register(registry, BIOME_SOURCE_ID" not in registries:
        raise SystemExit("mc1201 biome-source bootstrap registration missing")
    if "Registry.register(registry, CHUNK_GENERATOR_ID" not in registries:
        raise SystemExit("mc1201 chunk-generator bootstrap registration missing")



def verify_mc1201_fabric_resource_loader_dependency():
    gradle_props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    build_gradle = (ROOT / "versions/1.20.1/fabric/build.gradle").read_text(encoding="utf-8")
    fabric_mod = json.loads((ROOT / "versions/1.20.1/fabric/src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))

    if "mc1201_fabric_api_version=0.92.2+1.20.1" not in gradle_props:
        fail("mc1201 Fabric API version is not pinned to 0.92.2+1.20.1")
    if "net.fabricmc.fabric-api:fabric-api" not in build_gradle:
        fail("mc1201-fabric does not compile/runtime-depend on Fabric API")
    if "fabric-api" not in fabric_mod.get("depends", {}):
        fail("fabric.mod.json does not declare the Fabric API runtime dependency")

    preset = ROOT / "families/mc1201/common/src/main/resources/data/flterraforged/worldgen/world_preset/flterraforged.json"
    normal_tag = ROOT / "families/mc1201/common/src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json"
    if preset.exists() and normal_tag.exists() and "fabric-api" not in fabric_mod.get("depends", {}):
        fail("world preset resources are present without Fabric API/resource-loader support")

if __name__ == "__main__":
    main()
