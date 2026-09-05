#!/usr/bin/env python3
"""Verify the R45 one-time final wet reconciliation architecture."""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
WORLDGEN = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"
API = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
STANDARD = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/materializer/standard"
GENERATOR = WORLDGEN / "FlTerraForgedChunkGenerator.java"
FINAL_WET = WORLDGEN / "FinalWetReconciliationPass.java"
TERRAIN_WORLD = ROOT / "engine-api/src/main/java/dev/foucaultleon/flterraforged/engine/api/TerrainWorld.java"


def fail(message: str) -> None:
    raise SystemExit(f"ERROR: {message}")


def method_body(text: str, signature: str, following: str) -> str:
    start = text.find(signature)
    if start < 0:
        fail(f"missing method signature: {signature}")
    end = text.find(following, start)
    if end < 0:
        end = len(text)
    return text[start:end]


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//.*?$", "", text, flags=re.MULTILINE)


def main() -> None:
    for path in (GENERATOR, FINAL_WET, TERRAIN_WORLD):
        if not path.is_file():
            fail(f"missing R45 file: {path.relative_to(ROOT)}")

    generator = GENERATOR.read_text(encoding="utf-8")
    executable_generator = strip_comments(generator)
    final_wet = FINAL_WET.read_text(encoding="utf-8")
    materializer = (API / "BlockMaterializer.java").read_text(encoding="utf-8")
    delegate = (API / "DelegatingBlockMaterializer.java").read_text(encoding="utf-8")
    terrain_world = TERRAIN_WORLD.read_text(encoding="utf-8")
    standard = (STANDARD / "VanillaBlockMaterializer.java").read_text(encoding="utf-8")

    if "createStructurePlacementCalculator(" in executable_generator:
        fail("R45 must not reintroduce the early structure-placement Engine/BiomeSource bind")

    build_surface = method_body(
        executable_generator,
        "public void buildSurface(",
        "public void carve(",
    )
    for forbidden in (
        "HydrologyFillPass",
        "hydrologyFillPass",
        "FinalWetReconciliationPass",
        "finalWetReconciliationPass",
        "Heightmap.populateHeightmaps",
    ):
        if forbidden in build_surface:
            fail(f"buildSurface performs forbidden intermediate reconstruction: {forbidden}")

    carve = method_body(
        executable_generator,
        "public void carve(",
        "public void populateEntities(",
    )
    if "vanilla.carve(" not in carve:
        fail("R45 carve must delegate the vanilla carver")
    for forbidden in (
        "HydrologyCarverGuard",
        "hydrologyCarverGuard",
        "HydrologyFillPass",
        "hydrologyFillPass",
        "FinalWetReconciliationPass",
        "finalWetReconciliationPass",
        "Heightmap.populateHeightmaps",
    ):
        if forbidden in carve:
            fail(f"carve performs forbidden per-carver reconstruction: {forbidden}")

    features = method_body(
        executable_generator,
        "public void generateFeatures(",
        "public void getDebugHudText(",
    )
    final_call = features.find("finalWetReconciliationPass.apply(chunk, terrain);")
    vanilla_features = features.find("super.generateFeatures(world, chunk, structureAccessor);")
    if final_call < 0:
        fail("generateFeatures is missing the one-time final wet pass")
    if vanilla_features < 0 or final_call > vanilla_features:
        fail("final wet reconciliation must run before structures/native biome features")
    if executable_generator.count("finalWetReconciliationPass.apply(chunk, terrain);") != 1:
        fail("R45 must invoke final wet reconciliation exactly once")

    for required in (
        "world.sampleTile(pos.getStartX(), pos.getStartZ(), CHUNK_SIZE)",
        "materializer.hasFinalWetEnvelope(sample, x, z)",
        "materializer.permitsFinalWetFlow",
        "materializer.finalWetState",
        "target.getFluidState().isEmpty()",
    ):
        if required not in final_wet:
            fail(f"FinalWetReconciliationPass missing invariant: {required}")
    for forbidden in (
        "hydrologyBedState",
        "hydrologySealState",
        "substrateState",
        "surfaceSealState",
        "fillerState",
        "smoothedHydrologyBedY",
        "hydrologyGapBedY",
        "world.sample(",
    ):
        if forbidden in final_wet:
            fail(f"FinalWetReconciliationPass may not reconstruct solid geometry: {forbidden}")

    for required in (
        "default boolean hasFinalWetEnvelope(",
        "MaterializerGeometry.surfaceGeometry(this, sample, x, z)",
        "default boolean permitsFinalWetFlow(",
        "default BlockState finalWetState(",
    ):
        if required not in materializer:
            fail(f"BlockMaterializer missing R45 wet-envelope hook: {required}")
    for required in (
        "delegate.hasFinalWetEnvelope",
        "delegate.permitsFinalWetFlow",
        "delegate.finalWetState",
    ):
        if required not in delegate:
            fail(f"DelegatingBlockMaterializer does not forward: {required}")

    if "default TerrainSample[] sampleTile(" not in terrain_world:
        fail("Engine API must expose additive tile sampling for the final pass")

    if "coastUsesLandSurface" not in standard or "NaturalMaterialField.sample" not in standard:
        fail("standard materializer is missing coherent coast-to-land material blending")

    print("OK: R45 performs one fluid-only final wet reconciliation after carvers and before features")


if __name__ == "__main__":
    main()
