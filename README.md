# FlTerraForged

FlTerraForged ist eine neu aufgebaute, multi-versionale Worldgen-Plattform für
Minecraft. Das Projekt wird bewusst **nicht** auf der alten TerraForged-
Repositorystruktur weitergeführt.

## Status dieses Snapshots

`0.1.0-SNAPSHOT` ist der Architektur- und Engine-API-Snapshot.

Enthalten:

- reine Java-17-Engine-API ohne Minecraft-, Fabric-, NeoForge- oder TerraBlender-Abhängigkeit;
- Engine-Provider-/Lifecycle-Vertrag;
- Capability-System für optionale Engine-Daten;
- Terrain-, Klima- und Fluss-Datenmodell einschließlich fractional surface height;
- ServiceLoader-basierte Engine-Erkennung im `common`-Modul;
- Zielmatrix für alle stabilen Minecraft-Versionen von 1.20.1 bis 26.2 sowie genau den aktuellen Snapshot;
- vorbereitete Family-/Version-/Loader-Struktur;
- vorbereitete optionale Kompatibilitätsbereiche für TerraBlender, Conquest Reforged und Layer-Provider;
- Architektur- und Matrix-Prüfungen.

Noch **nicht** enthalten:

- TerraForged-/ReTerraForged-/FreeTerraForged-Quellcode;
- konkrete Default-Engine;
- Minecraft-Worldgen-Adapter;
- Fabric- oder NeoForge-Builds;
- TerraBlender-, Conquest- oder AronaLayers-Integration.

## Architektur

```text
External Engine
      |
      v
flterraforged-engine-api  (pure Java 17)
      |
      v
FlTerraForged common
      |
      +--> Minecraft API family
      |        |
      |        +--> Fabric
      |        +--> NeoForge
      |
      +--> optional biome integration
      +--> optional surface/layer providers
```

Die wichtigste Portierungsregel lautet:

```text
common -> platform-common -> family -> family/platform -> exact version
```

Versionsspezifischer Code ist die letzte, nicht die erste Lösung.

## Build

Der Snapshot verwendet Gradle 9.5.1 und kompiliert die Engine-API mit
`--release 17`.

Mit lokal installiertem Gradle:

```bash
gradle clean check
```

Zusätzliche Architekturprüfung:

```bash
python3 tools/verify-layout.py
```

Der Gradle Wrapper wird absichtlich noch nicht als binäres Artefakt in diesem
Snapshot mitgeliefert. Er kann nach dem ersten Checkout mit Gradle 9.5.1 erzeugt
werden:

```bash
gradle wrapper --gradle-version 9.5.1
```

## Nächster Schritt

Nach Annahme dieses API-Vertrags wird das separate Repository
`FlTerraForged-Engine` gegen `flterraforged-engine-api` implementiert. Erst
danach wird der Minecraft-Worldgen-Port begonnen.
