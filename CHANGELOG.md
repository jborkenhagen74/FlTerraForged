# Changelog

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
