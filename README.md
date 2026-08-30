# FlTerraForged

FlTerraForged is the Minecraft integration layer for an external and replaceable
terrain engine. Engines communicate with FlTerraForged exclusively through the
Java-only `flterraforged-engine-api` SPI.

## Current snapshot

`0.1.0-SNAPSHOT` establishes the architecture only. It intentionally does not
yet contain TerraForged/ReTerraForged/FreeTerraForged world-generation code.

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
