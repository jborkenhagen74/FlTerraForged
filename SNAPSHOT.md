# Snapshot

Current repository snapshot: **0.1.0-SNAPSHOT-r15**.

Current package revision: **r13** 0.1.0-SNAPSHOT

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


## r3 distribution model

Engine API snapshots are distributed from the repository's public `maven` branch,
not GitHub Packages. The generated repository originates at
`build/maven-repository` and retains Maven's normal version directory structure.

### r7 integration checkpoint

Fabric 1.20.1 is the first compiled target in the matrix. It consumes the public external-engine Maven artifact while substituting the Engine POM's transitive API with the local `:engine-api` project. It embeds Engine/API/common dependencies explicitly, registers data-driven worldgen codecs and ships a selectable world preset.

- CI/Gradle runtime is Java 21; Minecraft 1.20.1 bytecode remains Java 17.


## Minecraft 1.20.1 integration status (r12)

The Fabric reference adapter is functionally connected to vanilla NoiseRouter/aquifers, surface rules, carvers, placed features/ores and mob population. FlTerraForged remains authoritative for surface shape, rivers, climate and biome routing.


## Minecraft 1.20.1 validation gate (r13)

`MC1201-TEST-MATRIX.md` defines the mandatory P0 and extended P1 test gates that must pass before the 1.20.1 Fabric adapter is used as the reference implementation for another Minecraft API family.

## r14 CI artifact

Successful Verify runs publish the installable Minecraft 1.20.1 Fabric JAR directly as a 30-day GitHub Actions artifact and expose its download link/digest in the job summary.


### Minecraft 1.20.1 registry bootstrap

Biome-source and chunk-generator codecs are registered from mixins into the vanilla bootstrap helpers before the built-in registries freeze; the normal Fabric entrypoint only validates the result.
