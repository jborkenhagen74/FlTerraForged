## r16 – MC 1.20.1 world-preset discovery fix

- Added the actual `data/flterraforged/worldgen/world_preset/flterraforged.json` datapack resource.
- Added `flterraforged:flterraforged` to the `minecraft:normal` world-preset tag without replacing Vanilla presets.
- Added German and English `generator.flterraforged.flterraforged` translations so the selector shows `FlTerraForged`.
- Extended layout verification to require preset, tag, and translations.

# Changelog

## 0.1.0-SNAPSHOT-r15

- Fix Minecraft 1.20.1 startup crash caused by registering `BIOME_SOURCE` and `CHUNK_GENERATOR` codecs from the late Fabric `main` entrypoint after built-in registries were frozen.
- Register both codecs during vanilla bootstrap via `BiomeSourcesMixin` and `ChunkGeneratorsMixin`.
- Keep `ModInitializer` read-only: it now validates that both bootstrap registrations succeeded.
- Add a layout regression check preventing late worldgen codec registration from returning.


## 0.1.0-SNAPSHOT-r14

- Extended the GitHub Actions verify job to publish the installable Minecraft 1.20.1 Fabric mod JAR after every successful build (push, pull request and manual dispatch).
- Uses `actions/upload-artifact@v7.0.1` on the Node 24 runtime and uploads the JAR directly with `archive: false`.
- Filters sources, Javadoc, dev and shadow artifacts and fails if the build produces zero or multiple installable candidates.
- Renames the downloadable test JAR to include the short commit SHA, retains it for 30 days and writes its authenticated download link plus SHA-256 digest to the GitHub Actions job summary.
- Added layout regression checks for the CI artifact contract.

## 0.1.0-SNAPSHOT-r13

- Added `MC1201-TEST-MATRIX.md`, a gated functional validation matrix for the Minecraft 1.20.1 Fabric reference adapter.
- Covers startup/preset decoding, Engine terrain, climate/biomes, rivers/coasts, NoiseRouter/caves/aquifers, surface rules, vanilla features/ores, structures, chunk boundaries, determinism, performance and multiplayer lifecycle.
- Defines fixed regression seeds, evidence requirements, P0/P1 severity, release criteria and a reusable defect template.

## 0.1.0-SNAPSHOT-r11

- Run the complete Gradle multi-project build on Java 21 because Fabric Loom 1.17.x is itself compiled for Java 21.
- Install Java 17 alongside Java 21 in CI and keep the Minecraft 1.20.1 source/toolchain target at Java 17.
- Apply the same Java 21 Gradle runtime to the Engine API Maven publish job because Gradle configures the Loom-backed `mc1201-fabric` subproject even when only `:engine-api:publish` is requested.
- Add layout verification for the split build-runtime/toolchain contract.

## 0.1.0-SNAPSHOT-r10

- Fixed the Minecraft 1.20.1 seed-access bridge for Fabric Mixins.
- `NoiseConfig` is a concrete/final Minecraft type, so javac rejects a direct `instanceof NoiseConfigSeedAccess` check even though Mixin adds that interface at runtime.
- `EngineWorldSession` now performs the Mixin bridge cast through `Object` and converts a missing Mixin into a descriptive `IllegalStateException`.
- Added a layout regression check for the bridge so this compile-time incompatibility cannot be reintroduced accidentally.

## 0.1.0-SNAPSHOT-r9

- Fixed `mc1201-fabric` dependency resolution when consuming the external Engine snapshot.
- The external Engine POM declares `flterraforged-engine-api` transitively; the host build now excludes that remote transitive module and uses the local `:engine-api` project instead.
- Keeps Loom jar-in-jar deterministic: Engine is included non-transitively, while `:engine-api` and `:common` are embedded explicitly.
- Added layout regression checks so the 1.20.1 adapter cannot accidentally reintroduce a remote API dependency during the host build.

## 0.1.0-SNAPSHOT-r8

- Fixed the Minecraft 1.20.1 Fabric resource source-set configuration: `srcDirs(...)` appended the conventional `src/main/resources` root a second time, causing `fabric.mod.json` to be copied twice by Gradle 9.7.1.
- `mc1201-fabric` now uses `setSrcDirs(...)` for Java and resource roots, making each source root unique.
- CI now runs Gradle with `--warning-mode all` so any remaining Gradle/Loom deprecation is printed with its actual source instead of only the generic Gradle 10 compatibility summary.


- Added the first real Minecraft binding: Fabric 1.20.1.
- Added `FlTerraForgedChunkGenerator` and `FlTerraForgedBiomeSource`.
- Added `NoiseConfigMixin` to bind the external engine to the actual world seed.
- Added the data-driven `flterraforged:flterraforged` world preset with vanilla Nether/End.
- Added native biome routing from engine terrain/climate signals.
- Added FEF-style external Engine Maven resolution and Loom jar-in-jar packaging.
- Added static verification for the MC 1.20.1 reference binding.
- Documented current limitations (carvers, full surface rules, aquifers and density integration are follow-ups).


## 0.1.0-SNAPSHOT-r6

- Added explicit `@param` documentation to all public record compact constructors.
- Fixes Java 17 Javadoc `-Werror` failures (`18 warnings`) during `engine-api:javadoc` / Maven publishing.


## 0.1.0-SNAPSHOT

- Fix: declare the JUnit Platform launcher on the test runtime classpath for Gradle 9.x.

- Initialized clean FlTerraForged architecture.
- Added pure Java 17 `flterraforged-engine-api`.
- Added engine provider, lifecycle, capability and terrain sample contracts.
- Preserved fractional surface height as a first-class API value.
- Added ServiceLoader-ready engine registry in `common`.
- Added provisional Minecraft family matrix from 1.20.1 through 26.2 plus current snapshot.
- Enforced a maximum of one snapshot target.
- Added Fabric/NeoForge, family, exact-version and optional compatibility skeletons.
- Added isolation and layout verification.
- No upstream source code imported yet.

## 0.1.0-SNAPSHOT-r2

- Publishes `flterraforged-engine-api` to GitHub Packages after a successful `main` build.
- Uses `GITHUB_TOKEN` with repository-scoped `packages: write` permission in CI.
- Aligns CI on Gradle 9.7.1.


### r3
- Replaced GitHub Packages publishing with the FEF-style public Maven repository model.
- `engine-api` now publishes into `build/maven-repository`.
- `develop` mirrors the generated Maven repository to the public `maven` branch.
- Added `develop`/`main` workflow separation; snapshots publish only from `develop`.
- Consumers no longer require GitHub Packages credentials.


### r4
- Updated GitHub Actions to Node 24 compatible releases: `actions/checkout@v6`, `actions/setup-java@v6`, and `gradle/actions/setup-gradle@v6`.
- Pinned `peaceiris/actions-gh-pages@v4.1.0`, whose action runtime uses Node 24.
- Removes Node 20 deprecation warnings from the build and Maven-branch publishing workflow.

## 0.1.0-SNAPSHOT-r5

- Added complete Javadoc coverage for all public Engine API members.
- Added package-level API documentation.
- Configured Javadoc with `-Werror` so Maven publication cannot silently emit Javadoc warnings.

## r12 - functional Minecraft 1.20.1 worldgen binding

- Replaced the column-only 1.20.1 noise fill with a hybrid vanilla/Engine pipeline.
- Added `VanillaWorldgenDelegate` around Minecraft 1.20.1 `NoiseChunkGenerator`.
- Added `EngineDensityBridge` to remap vanilla NoiseRouter/aquifer columns to Engine heights.
- Delegated vanilla `buildSurface`, AIR/LIQUID `carve`, and `populateEntities`.
- Kept vanilla feature/ore generation on the inherited `ChunkGenerator.generateFeatures` path.
- Added `EngineSurfaceGuard` for Engine-specific coast/river/snow semantics and surface fallback.
- Added `MC1201-FUNCTIONAL-WORLDGEN.md` documenting stage ownership and known boundaries.
