# FlTerraForged R47

R47 builds directly on R46 and is intended to be paired with FlTerraForged-Engine R38.

## Coast material lock

Dry `COAST` terrain no longer uses a radial moisture/noise threshold to switch between sand and the general land surface. Coast remains coast material; moisture is used by decoration instead. This removes the circular grass islands that could appear inside otherwise continuous sand areas.

## Component-aware carving water

- Carved cells are seeded from both vertical surface breaches and horizontal contact with an existing water column.
- Ocean/sea water has authority over lake water; lake water has authority over river water.
- A high river therefore cannot raise a lower receiving sea/lake through one connected cave component.
- River-only components keep the local hydraulic ceiling used by R46 so waterfall separation is retained.

## Surface-carving frequency

Cave and ravine origin rates are reduced. Dry coasts, lake shores and river banks keep a six-block roof, ordinary dry terrain keeps four blocks and steep/mountain terrain keeps two. Underwater openings may still breach naturally.

## Variable-height providers

Carver roof and surface limits use `MaterializerGeometry.surfaceGeometry(...)` instead of assuming the provider's top material fills a complete block. Flood materialization also respects `permitsFinalWetFlow(...)` / `finalWetState(...)` where a provider can carry water through custom partial geometry.

## Shoreline decoration

The standard materializer now applies a broad-patch shoreline decorator after existing watercourse decoration. Shore sections deterministically alternate among bare sediment, rocky banks, sparse dry growth and lush shrub/fern sections. External providers are unaffected and can continue to own their own material palettes and geometry-aware decoration.
