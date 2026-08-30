# FlTerraForged Architektur

## 1. Feste Architekturentscheidungen

1. Die Terrain-Engine ist extern und austauschbar.
2. FlTerraForged kennt konkrete Engines nur über `flterraforged-engine-api`.
3. Die Engine-API ist Minecraft-unabhängig und zielt auf Java 17.
4. TerraBlender ist eine optionale Biome-Integration, keine Kernabhängigkeit.
5. Conquest Reforged und Layer-Mods werden über optionale Surface-/Layer-Provider angebunden.
6. Änderungen werden zuerst in gemeinsamem Code, dann in Versionsfamilien und nur zuletzt in exakten Versionen implementiert.
7. Zwischen-Snapshots werden nicht konserviert: Die Matrix enthält höchstens einen Snapshot, den jeweils aktuellen.

## 2. Modulgrenzen

### `engine-api`

Darf nur Java-SE-APIs verwenden. Verboten sind insbesondere:

- `net.minecraft.*`
- `net.fabricmc.*`
- `net.neoforged.*`
- `net.minecraftforge.*`
- `com.mojang.serialization.*`

Die API beschreibt ausschließlich semantische Terrain-Daten und Lifecycle.

### `common`

Enthält FlTerraForged-eigene Orchestrierung, Engine-Auswahl und später
Minecraft-unabhängige bzw. breit gemeinsame Worldgen-Logik.

### `platforms`

Loader-spezifische gemeinsame Implementierungen für Fabric bzw. NeoForge.

### `families`

Minecraft-API-Familien. Die im ersten Snapshot angelegten Grenzen sind bewusst
als **provisional** markiert und werden durch den TerraForged/ReTerraForged/
FreeTerraForged-Diff verifiziert.

### `versions`

Nur echte Einzelversions-Ausnahmen. Jeder Zielordner existiert von Anfang an,
bleibt aber im Normalfall ohne Java-Code.

### `compat`

Optionale Integrationen. Keine davon darf für den Kernstart erforderlich sein.

## 3. Engine-Vertrag

Eine Engine wird über `EngineProvider` entdeckt. Ein Provider erstellt eine
`TerrainEngine`; diese öffnet pro Welt eine `TerrainWorld`.

```text
EngineProvider
    -> TerrainEngine
        -> TerrainWorld(seed/world bounds)
            -> TerrainSample(x,z)
```

### Determinismus

Für denselben Seed, dieselbe Konfiguration und dieselben Koordinaten muss ein
Sample unabhängig von Aufrufreihenfolge und Thread-Scheduling reproduzierbar
sein.

### Thread-Safety

`TerrainWorld.sample(x, z)` muss parallel aufrufbar sein. Engines dürfen intern
Caches verwenden, müssen deren Synchronisierung jedoch selbst gewährleisten.

### Fractional Surface Height

`TerrainSample.surfaceHeight()` ist `double`, nicht `int`. Damit kann eine
Engine z. B. `123.625` liefern. Spätere Layer-Provider können daraus
Teilblockhöhen ableiten, ohne das Density Field nachträglich rekonstruieren zu
müssen.

## 4. Capability-Modell

Nicht jede austauschbare Engine muss jedes Zusatzsignal liefern. Deshalb werden
optionale Fähigkeiten über `EngineCapability` angekündigt.

Der Kernwert `surfaceHeight` ist verpflichtend. Zusatzwerte können mit `NaN`
bzw. einem `UNAVAILABLE`-Sample gekennzeichnet werden, falls die passende
Capability fehlt.

## 5. Biome-Integration

Zielbild:

```text
BiomeIntegration
    +-- NativeBiomeIntegration       (Default)
    +-- TerraBlenderBiomeIntegration (optional)
```

TerraBlender darf weder Engine-API noch Engine-Implementierung beeinflussen.

## 6. Surface-/Layer-Integration

Die späteren AronaLayers-Erkenntnisse werden nicht als harte Abhängigkeit,
sondern als abstrahierte Provider umgesetzt:

```text
SurfaceProvider
LayerProvider
DecorationProvider
```

Geplante Provider:

- Vanilla
- Conquest Reforged
- VanillaLayerPlus oder andere Layer-Mods
- externe Drittanbieter

## 7. Portierungsreihenfolge

1. Engine API stabilisieren.
2. Default-Engine in separatem Repository implementieren.
3. Referenzziele 1.20.1, 1.21.1 und 26.2 anbinden.
4. aktuellen Snapshot anbinden.
5. Familiengrenzen anhand realer API-Diffs korrigieren.
6. Zwischenversionen familienweise ergänzen.
7. optionale TerraBlender-Integration.
8. Surface-/Layer-/Conquest-Provider.

## Package ownership and CI boundary

`FlTerraForged` owns `flterraforged-engine-api` and is the only repository that builds and publishes that API in CI. External engines consume a published API version. This prevents engine workflows from duplicating or implicitly rebuilding the host project.


## Host dependency substitution boundary

When FlTerraForged itself consumes an external Engine artifact, the Engine's transitive `flterraforged-engine-api` dependency is excluded. The host build owns that API and supplies the local `:engine-api` project instead. This prevents the host CI from depending on an already-published API snapshot and guarantees that Engine, common code and the Minecraft adapter compile against the same API classes.

## Maven boundary

`engine-api` is published to `build/maven-repository` and mirrored by CI to the
public `maven` branch. This deliberately avoids a runtime or build dependency on
GitHub Packages authentication. External engines compile against the Maven
coordinate only and do not need the FlTerraForged source tree.

## Minecraft 1.20.1 reference binding

The first real adapter lives in the `mc1201` family and is packaged by `versions/1.20.1/fabric`. The family-common layer owns engine session binding, biome routing and the custom chunk generator. Fabric owns only codec registration, Loader entrypoint and the `NoiseConfig` seed-capture mixin. The chunk generator and biome source share one `TerrainWorld`, so terrain and biome decisions cannot drift to different engine instances/seeds.

The reference adapter currently materializes simple columns directly. Minecraft density functions, aquifers, carvers and full surface-rule delegation are deliberately left as the next integration stage rather than leaking those APIs into the external engine.

## Minecraft 1.20.1 functional reference pipeline

The 1.20.1 Fabric adapter is intentionally hybrid rather than a second terrain
engine inside the Minecraft module:

```text
FlTerraForged Engine TerrainWorld
             |
             | surface height / climate / rivers
             v
      EngineDensityBridge
             ^
             |
Vanilla NoiseChunkGenerator
  NoiseRouter + aquifers
             |
             v
   vanilla Surface Rules
             |
             v
      EngineSurfaceGuard
             |
             v
      vanilla Carvers
             |
             v
  vanilla Features / Ores
```

The Minecraft-family layer may depend on Minecraft classes and vanilla worldgen.
The Engine API and external Engine remain Java-only and are not allowed to import
Minecraft, Fabric, NeoForge or Mojang Codec types.

### Fabric 1.20.1 resource loading

The 1.20.1 Fabric adapter has an explicit runtime dependency on Fabric API 0.92.2+1.20.1. Its `fabric-resource-loader-v0` module exposes the mod JAR's `data/` tree as a built-in data pack during world-creation registry loading. This is required for the `flterraforged:flterraforged` world preset and its `minecraft:normal` tag contribution to reach the dynamic `WORLD_PRESET` registry.

