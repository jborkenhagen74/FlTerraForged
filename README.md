# FlTerraForged

FlTerraForged is a clean-room project structure for a multi-version Minecraft terrain generator integration. The terrain engine is external and replaceable through the Java-only `flterraforged-engine-api` SPI.

## Current snapshot

`0.1.0-SNAPSHOT` establishes the architecture only. It intentionally does not yet contain TerraForged/ReTerraForged/FreeTerraForged world-generation code.

## Modules

- `engine-api` — Java 17 API/SPI, independent of Minecraft and loaders.
- `common` — Minecraft-facing common integration layer.
- `platforms` — Fabric and NeoForge loader-specific code.
- `families` — Minecraft API-family adaptations.
- `versions` — exact-version overrides only when a family cannot cover a change.
- `compat` — optional integrations such as TerraBlender, Conquest Reforged and layered surfaces.

## Engine API publication

`engine-api` is owned and published by this repository. CI performs `clean check` first. Only a successful push to `main` publishes:

```text
dev.foucaultleon:flterraforged-engine-api:0.1.0-SNAPSHOT
```

to:

```text
https://maven.pkg.github.com/jborkenhagen74/FlTerraForged
```

The external engine repository consumes that package; it does not check out or build FlTerraForged in CI.

GitHub Actions publishing uses the repository-scoped `GITHUB_TOKEN`. No long-lived publishing token is required for the normal workflow.

## Local development

Build and test:

```bash
gradle --no-daemon clean check
```

Publish the API to Maven Local only when explicitly needed for local testing:

```bash
gradle --no-daemon :engine-api:publishToMavenLocal
```

The default Engine repository instead supports a Gradle composite build, which is preferred when API and engine are edited together.
