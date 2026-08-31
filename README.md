# FlTerraForged

FlTerraForged is the Minecraft integration layer for an external and replaceable
terrain engine. Engines communicate with FlTerraForged exclusively through the
Java-only `flterraforged-engine-api` SPI.

## Current snapshot

`0.1.0-SNAPSHOT` contains the Engine API architecture and the first executable
Minecraft 1.20.1 Fabric reference binding. The external terrain implementation remains
in the separate `FlTerraForged-Engine` repository.

## Modules

- `engine-api` — Java 17 API/SPI, independent of Minecraft and loaders.
- `common` — Minecraft-facing common integration layer.
- `platforms` — Fabric and NeoForge loader-specific code.
- `families` — Minecraft API-family adaptations.
- `versions` — exact-version overrides only when a family cannot cover a change.
- `compat` — optional integrations such as TerraBlender, Conquest Reforged and layered surfaces.

## Engine API publication

`FlTerraForged` owns and publishes the Engine API. Development snapshots from
`develop` are first written to:

```text
build/maven-repository
```

and GitHub Actions mirrors that Maven repository to the public `maven` branch.
The public repository URL is:

```text
https://raw.githubusercontent.com/jborkenhagen74/FlTerraForged/maven/
```

Current coordinate:

```text
dev.foucaultleon:flterraforged-engine-api:0.1.0-SNAPSHOT
```

Consumers do not need GitHub Packages credentials while the repository remains
public. The publishing workflow itself only needs the repository-scoped
`GITHUB_TOKEN` to push the generated files to the `maven` branch.

## Local development

Build and test:

```bash
gradle --no-daemon clean check
```

Publish the API to the FEF-style local build repository:

```bash
gradle --no-daemon :engine-api:publish
```

This creates the normal Maven directory hierarchy below:

```text
build/maven-repository/dev/foucaultleon/flterraforged-engine-api/
```

Multiple API versions can coexist there in their normal Maven version
directories.

You can also use Gradle's standard Maven Local task when explicitly wanted:

```bash
gradle --no-daemon :engine-api:publishToMavenLocal
```

## Branch model

```text
develop -> build/test -> publish 0.1.0-SNAPSHOT to maven branch
main    -> build/test only (release publishing will be added separately)
PR      -> build/test only
```

## First Minecraft binding

`versions/1.20.1/fabric` is the first executable reference target. It registers a custom chunk generator and biome source, binds both to the same external `TerrainWorld`, and exposes the world preset `flterraforged:flterraforged`. The current 1.20.1 adapter delegates Minecraft's vanilla NoiseRouter/aquifer substrate, surface rules, carvers and entity population. It reconciles the substrate with Engine heights without vertically translating caves or underground layers. Snapshot r26 keeps the r25 basin hydrology and materializes climate-weighted, basin-aware Engine hydrology: terrain-guided rivers/streams, guaranteed wet-channel depth and ponds/lakes through the shared hydrology column rule. Dry catchments contribute less runoff, and persistent desert rivers receive a narrow plains/grass riparian fringe so vanilla vegetation can follow the banks. Vanilla features and ores remain on the inherited biome-generation path. See `MC1201-FIRST-BINDING.md` and `MC1201-FUNCTIONAL-WORLDGEN.md`. Execute `MC1201-TEST-MATRIX.md` before treating the adapter as the reference for another Minecraft family.


### Build JVM for the 1.20.1 reference adapter

- Gradle/Loom runtime: Java 21
- Minecraft 1.20.1 compilation/toolchain target: Java 17

These are intentionally different: Loom 1.17.x requires Java 21 to load, while the generated 1.20.1 mod remains targeted to Java 17.

## CI test artifact

Every successful GitHub Actions `Verify` job now uploads the installable Minecraft 1.20.1 Fabric JAR as a direct workflow artifact. The file is named `FlTerraForged-1.20.1-Fabric-<short-sha>.jar`, retained for 30 days and linked from the job summary together with GitHub's SHA-256 artifact digest. Sources, Javadoc, dev and shadow JARs are excluded.

Use the downloaded JAR directly in a Minecraft 1.20.1 Fabric instance; the external Engine, Engine API and common module are embedded by the Loom build.

#
### Minecraft 1.20.1 Fabric runtime dependency

The reference binding requires **Fabric API 0.92.2+1.20.1 or newer compatible 1.20.1 release** at runtime. In particular, `fabric-resource-loader-v0` is required so the bundled `data/` resources are exposed as a mod data pack and `flterraforged:flterraforged` can be loaded into the dynamic `WORLD_PRESET` registry.

For a client or server installation use:

- Minecraft 1.20.1
- Fabric Loader
- Fabric API 0.92.2+1.20.1
- FlTerraForged 1.20.1 Fabric

## Minecraft 1.20.1 world preset

The Fabric 1.20.1 artifact ships `flterraforged:flterraforged` as a data-driven world preset and contributes it to `minecraft:normal`. It should appear as **FlTerraForged** in the Create World → World → World Type selector.
### r24 desert balance and cave-safe hydrology

The 1.20.1 native biome router now reserves desert for `temperature > 0.80` and
`moisture < 0.28`; hot but merely semi-dry terrain routes to plains instead. After each delegated
vanilla carver step, `HydrologyCarverGuard` restores only Engine-owned river/lake/pond water, a
five-block bed seal and the immediately adjacent subsurface bank wall. This preserves normal caves
away from surface water while preventing carvers from draining or shredding hydrologic features.
Engine r21+ is recommended for the matching bank-contained mountain-river profile.



### r25 flat lakes and initial materializer abstraction

Engine r22 groups connected depression-fill nodes into basins with one constant water level and
exports dry shoreline transitions as `LAKE_SHORE`. The mc1201 host replaces the old fixed hydrology
quantizer with `BlockMaterializer`. The built-in `VanillaBlockMaterializer` uses full-block resolution and, for
a shallow lake whose bed and water surface would otherwise round to the same Y, lowers only the
materialized bed enough to fit one real water block. Density shaping, column samples, surface repair
and post-carver protection all use that same decision. The contract already exposes vertical
resolution, partial-block support and waterlogging support so a Conquest adapter can later use
half/quarter blocks without changing Engine geometry.


### r26 replaceable materializers

Minecraft block realization is now a replaceable host SPI. `flterraforged:vanilla` is bundled as the standard implementation. Fabric add-ons can expose `BlockMaterializerProvider` through the `flterraforged:materializer` entrypoint and are selected with `config/flterraforged/materializer.properties`. The Engine has no dependency on this mechanism. See `MATERIALIZER-SPI.md` and `examples/materializer-addon`.
