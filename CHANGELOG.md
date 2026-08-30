# Changelog

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
