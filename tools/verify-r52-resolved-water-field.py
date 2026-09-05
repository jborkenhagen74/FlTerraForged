#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
API = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/api/mc1201/materializer"
WORLDGEN = ROOT / "families/mc1201/common/src/main/java/dev/foucaultleon/flterraforged/minecraft/mc1201/worldgen"


def require(path: Path, *needles: str) -> None:
    text = path.read_text(encoding="utf-8")
    missing = [needle for needle in needles if needle not in text]
    if missing:
        raise SystemExit(f"{path}: missing R52 invariants: {missing}")


def forbid(path: Path, *needles: str) -> None:
    text = path.read_text(encoding="utf-8")
    present = [needle for needle in needles if needle in text]
    if present:
        raise SystemExit(f"{path}: forbidden R52 patterns remain: {present}")


def main() -> None:
    geometry = API / "MaterializerGeometry.java"
    materializer = API / "BlockMaterializer.java"
    columns = WORLDGEN / "ColumnComposer.java"
    density = WORLDGEN / "EngineDensityBridge.java"
    surface = WORLDGEN / "EngineSurfaceGuard.java"
    generator = WORLDGEN / "FlTerraForgedChunkGenerator.java"
    marine = WORLDGEN / "MarineEnvironmentCache.java"
    legacy_fill = WORLDGEN / "HydrologyFillPass.java"

    for path in (geometry, materializer, columns, density, surface, generator, marine):
        if not path.is_file():
            raise SystemExit(f"missing R52 source: {path}")
    if legacy_fill.exists():
        raise SystemExit("obsolete HydrologyFillPass must not exist in R52")

    require(
        geometry,
        "hasMaterializableWater",
        "capabilities().waterlogging()",
        "firstWaterY",
        "geometry.blockY() + 1",
    )
    require(
        materializer,
        "MaterializerGeometry.hasMaterializableWater",
        "finalWetState",
        "permitsFinalWetFlow",
    )
    require(
        columns,
        "MaterializerGeometry.surfaceGeometry(materializer, sample, x, z)",
        "MaterializerGeometry.firstWaterY",
        "finalWetState",
        "surfaceTop(TerrainSample sample, int x, int z)",
    )
    require(
        density,
        "MaterializerGeometry.surfaceGeometry(materializer, sample, blockX, blockZ)",
        "int targetSurfaceY = geometry.blockY()",
    )
    require(
        surface,
        "MaterializerGeometry.surfaceGeometry(materializer, sample, x, z)",
        "applySurfaceWaterlogging",
        "finalWetState",
    )
    require(
        generator,
        "columns.surfaceTop(sample, x, z)",
        "columns.worldSurfaceTop(sample, x, z)",
    )
    require(
        marine,
        "MaterializerHeightQuantizer.exclusiveFluidTop",
        "MaterializerGeometry.hasMaterializableWater",
    )

    # No host-side second hydrology solution may be reintroduced.
    for path in (columns, density, surface, generator, marine):
        forbid(path, "smoothedHydrologyBedY", "mayRepairHydrologyGap(")

    print("R52 resolved-water/provider-geometry invariants verified")


if __name__ == "__main__":
    main()
