# Minecraft 1.20.1 – erste FlTerraForged-Anbindung

## Ziel

Dieser Stand verbindet die externe `FlTerraForged-Engine` erstmals mit echter Minecraft-Worldgen. Die Referenzimplementierung ist Fabric 1.20.1; der Minecraft-Familiencode liegt bewusst in `families/mc1201/common`, damit NeoForge denselben Kern später wiederverwenden kann.

## Datenfluss

```text
NoiseConfig world seed
        |
        v
EngineWorldSession
        |
        v
TerrainWorld (external engine)
        |-----------------------|
        v                       v
FlTerraForgedChunkGenerator  FlTerraForgedBiomeSource
        |                       |
        v                       v
block columns               native biomes
```

ChunkGenerator und BiomeSource werden an dieselbe `TerrainWorld` gebunden. Der Seed wird in Minecraft 1.20.1 über `NoiseConfigMixin` aus dem privaten `NoiseConfig`-Konstruktor übernommen.

## Aktueller Funktionsumfang

- registrierter `flterraforged:chunk_generator`
- registrierte `flterraforged:biome_source`
- externes Engine-SPI via ServiceLoader
- `balanced` als Default-Engine-Preset
- Fractional Height -> Blockoberfläche
- einfache Stone/Dirt/Grass/Sand/Snow/Wasser-Spalten
- native Biome-Zuordnung aus Terrain + Climate
- Ocean/Coast/River-Berücksichtigung
- World-Preset `flterraforged:flterraforged`
- Nether und End bleiben Vanilla
- Debug-HUD-Daten des Engine-Samples

## Bewusste Grenzen dieses ersten Adapters

Noch nicht integriert sind Vanilla Carver/Caves, Surface Rules, Aquifers, Ores/Features, komplexe Fluid-Level für 3D Rivers sowie eine DensityFunction-Brücke. `buildSurface`, `carve` und `populateEntities` sind deshalb zunächst bewusst minimale/no-op Adapterpunkte.

Die River-Wasserhöhe ist vorläufig: Unterhalb des Meeresspiegels wird bis zum Vanilla-Sea-Level aufgefüllt; bei als `RIVER` klassifiziertem Hochlandterrain wird aus der Engine-Rivertiefe eine kleine lokale Wasserschicht approximiert. Eine explizite River-Water-Level-API kann diese Näherung später ersetzen.

## Build

```bash
gradle --no-daemon :mc1201-fabric:build
```

Das fertige Fabric-Mod-JAR liegt danach unter:

```text
versions/1.20.1/fabric/build/libs/
```

Die API-, Common- und Default-Engine-JARs werden mit Loom `include` als Jar-in-Jar eingebettet. Verbraucher müssen diese drei Artefakte deshalb nicht separat in den Mods-Ordner kopieren.

## Entwicklung mit lokalem Engine-Repository

Reihenfolge für die Engine-Auflösung:

1. `-Pflterraforged_engine_local_repository=/pfad/zum/maven-repository`
2. `FLTERRAFORGED_ENGINE_LOCAL_REPOSITORY`
3. automatisch `../FlTerraForged-Engine/build/maven-repository`, falls vorhanden
4. `mavenLocal()`
5. öffentliche `maven`-Branch von FlTerraForged-Engine

Die Remote-URL kann mit `flterraforged_engine_repository_url` bzw. `FLTERRAFORGED_ENGINE_REPOSITORY_URL` überschrieben werden.

## Welt erstellen

Singleplayer: Im Welt-Erstellen-Dialog erscheint das Preset **FlTerraForged** über den `minecraft:normal` World-Preset-Tag.

Dedicated Server:

```properties
level-type=flterraforged:flterraforged
```

Für bestehende Welten wird der Generator nicht automatisch umgestellt; die Referenzanbindung ist zunächst für neu erzeugte Welten gedacht.

## Gradle 9 resource roots

The version project replaces (`setSrcDirs`) Gradle's conventional Java/resource roots instead of appending them. This is important because `src/main/resources` is already a default resource root; appending it again makes `processResources` see `fabric.mod.json` twice and Gradle 9.7.1 fails on the duplicate.

CI uses `--warning-mode all` so Gradle/Loom deprecations are attributable to a concrete build script or plugin.

## Engine/API dependency boundary

The 1.20.1 host adapter consumes `dev.foucaultleon:flterraforged-engine` from the external Engine Maven repository, but it does **not** resolve that Engine POM's `flterraforged-engine-api` dependency remotely. FlTerraForged owns the API and therefore compiles against the local `:engine-api` project. The transitive API dependency is excluded from the Engine declaration, while Loom embeds the Engine non-transitively and embeds `:engine-api` and `:common` explicitly.

This keeps a clean checkout buildable without requiring a previously published API snapshot and guarantees one API copy/version inside the final Fabric jar.

## Mixin seed-access bridge (r10)

Minecraft 1.20.1 exposes `NoiseConfig` as a concrete type that does not declare
`NoiseConfigSeedAccess` at compile time. Fabric Mixin adds that interface only after
class transformation at runtime. Java therefore rejects a direct
`noiseConfig instanceof NoiseConfigSeedAccess` test as statically impossible.

The adapter intentionally bridges through `Object`:

```java
return (NoiseConfigSeedAccess) (Object) noiseConfig;
```

If the Mixin is not applied, the resulting `ClassCastException` is converted to a
descriptive `IllegalStateException` that points to `flterraforged.mixins.json`.


## Java runtime versus Minecraft target

The 1.20.1 adapter has two deliberately different Java requirements:

- **Gradle/Loom runtime:** Java 21. Fabric Loom 1.17.x is compiled for Java 21 and therefore cannot be loaded by a Gradle daemon running on Java 17.
- **Minecraft/mod bytecode target:** Java 17. The `mc1201-fabric` project keeps a Java 17 toolchain and `options.release = 17`, so the produced 1.20.1 mod remains Java-17-compatible.

CI installs both JDKs. Java 17 is installed first so Gradle can discover it as a compilation toolchain; Java 21 is installed last and therefore becomes `JAVA_HOME` for the Gradle process. The same rule applies to the Engine API publish job because Gradle configures all subprojects, including the Loom-backed 1.20.1 project, before executing `:engine-api:publish`.
