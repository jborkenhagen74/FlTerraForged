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
        ROOT / "versions/1.20.1/fabric/src/main/resources/data/flterraforged/worldgen/world_preset/flterraforged.json",
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
    if workflow.find("java-version: '17'") > workflow.find("java-version: '21'"):
        fail('Verify job must install Java 17 before activating Java 21 as JAVA_HOME')
    mc1201_build = (ROOT / 'versions' / '1.20.1' / 'fabric' / 'build.gradle').read_text(encoding='utf-8')
    if 'JavaLanguageVersion.of(17)' not in mc1201_build or 'options.release = 17' not in mc1201_build:
        fail('Minecraft 1.20.1 must continue targeting Java 17')

    print(
            f"OK: {len(targets)} targets, {len(snapshots)} snapshot, engine-api isolated, "
            "public Maven publishing configured, mc1201 Fabric functional binding present"
        )

if __name__ == "__main__":
    main()
