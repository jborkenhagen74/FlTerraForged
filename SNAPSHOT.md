# Snapshot 0.1.0-SNAPSHOT

Purpose: freeze the first architecture contract before importing upstream
worldgen code.

Acceptance criteria:

- [x] Engine API has no Minecraft/loader dependencies.
- [x] Engine API targets Java 17.
- [x] External engines have a Provider -> Engine -> World lifecycle.
- [x] Terrain samples preserve fractional heights.
- [x] Optional engine data is capability-based.
- [x] Engine discovery exists without binding a concrete engine.
- [x] TerraBlender is represented only as optional compatibility.
- [x] Fabric and NeoForge are both represented in the matrix skeleton.
- [x] Common -> family -> exact version policy is documented and verified.
- [x] Only one snapshot is present in the target matrix.
- [x] No upstream implementation source is imported in this snapshot.

## Build fix

The snapshot explicitly declares the JUnit Platform launcher as a test runtime dependency for Gradle 9.x.
