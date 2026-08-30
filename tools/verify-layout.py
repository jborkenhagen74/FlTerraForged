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

    print(f"OK: {len(targets)} targets, {len(snapshots)} snapshot, engine-api isolated, public Maven publishing configured")


if __name__ == "__main__":
    main()
