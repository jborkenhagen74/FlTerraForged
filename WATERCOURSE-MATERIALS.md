# Standardmaterialisierung für Wasserläufe

Stand: **0.1.0-SNAPSHOT-r31**, Minecraft-Familie **mc1201**

## Ziel

Der Standardmaterializer bildet Flüsse, Seen und ihre Ufer nicht mehr mit je einem einzigen Block
ab. Jede Oberflächen-Wasserstelle wird zunächst einem Höhen-/Klimaprofil und anschließend einer von
drei sichtbaren Zonen zugeordnet:

1. **Nasszone:** der feste, vollständig überflutete Gewässergrund;
2. **Feuchtzone:** die innere Uferzone an der Wasserlinie;
3. **Trockenzone:** der äußere Übergang in die normale Biomoberfläche.

Unter Feucht- und Trockenzone liegt zusätzlich ein profilspezifischer stabiler Füller. Die Auswahl
innerhalb einer Palette folgt einem verzerrten, niederfrequenten Materialfeld. Dadurch entstehen
breite Sedimentlinsen und Gesteinszüge statt fester 3×3-Zufallsfelder oder unabhängigem Rauschen pro
Block. Die äußere Uferzone ist 8–12 Blöcke breit und gibt mit abnehmendem Hydrologieeinfluss immer
mehr Spalten an die normale Biomoberfläche zurück.

## Höhen- und Klimaprofile

| Profil | Auswahl | Standardcharakter |
| --- | --- | --- |
| `high_alpine` | Y > 120 | Stein, Andesit, Bruchstein, Kies, Kalzit; keine Erde und kein Sand |
| `snowy_highland` | Y 90–120, kalt/taigaartig | Kies, Bruchstein, Schlamm, Diorit, Moosbruchstein, Packeis |
| `alpine` | Y 90–120, sonst | Kies, Stein, Andesit, Bruchstein, Kalzit |
| `forest` | Y 64–89, feucht/waldartig | Lehm, Kies, Sand, Schlamm, Moos, wurzelige Erde |
| `dark_forest` | Y 64–89, sehr feucht/dichter Wald | Schlamm, Lehm, Podsol, grobe Erde, Myzel, Moosbruchstein |
| `jungle` | Y 64–89, heiß/feucht | Schlamm, Lehm, Sand, Moos, selten Feuerkorallenblock |
| `midland` | Y 64–89, offene gemäßigte Landschaft | Kies, Lehm, Schlamm, Moos, Gras, wurzelige Erde |
| `plains` | Y 60–63, normales Flachland | Sand, Kies, Lehm, Schlamm, Gras |
| `dryland` | trocken/heiß bzw. Wüste/Badlands | Sand, roter Sand, Sandstein, rote Terrakotta; schmaler feuchter Saum mit Schlamm/vereinzelt Gras |
| `wetland` | sehr feucht bzw. Sumpf/Mangrove | Schlamm, Lehm, Mangrovenwurzeln, Moos, Moosbruchstein |
| `lush_underground` | Oberfläche unter Y 60, feucht/flach | Lehm, Moos, Schlamm, Sand, wurzelige Erde |
| `rocky_underground` | Oberfläche unter Y 60, trocken/steil | Kies, Tropfsteinblock, Stein, Granit |
| `deepslate` | Y < 0 | Tiefenschiefer, Bruchtiefenschiefer, Tuff, sehr selten Seelaterne |

Die Engine ist weiterhin eine 2,5-D-Oberflächenengine. Die Profile unter Y 60 gelten deshalb für
offene bzw. abgesenkte Oberflächengewässer. Echte unterirdische Höhlenflüsse benötigen später eine
eigene 3-D-Hydrologie und werden mit dieser Änderung nicht vorgetäuscht.

## Konfiguration

Die bisherigen globalen Schlüssel bleiben gültig und haben Vorrang:

```properties
blockset.river_bed=minecraft:gravel*5,minecraft:cobblestone*2
blockset.lake_bed=minecraft:clay*4,minecraft:mud*3,minecraft:gravel
blockset.riparian=minecraft:grass_block*3,minecraft:mud
blockset.lake_shore_wet=minecraft:grass_block,minecraft:moss_block
blockset.lake_shore_dry=minecraft:sand,minecraft:coarse_dirt
```

Fehlt ein globaler Schlüssel, wird automatisch das vollständige passende Standardprofil genutzt.
Einzelne Profile können gezielt überschrieben werden:

```properties
blockset.watercourse.alpine.bed=minecraft:stone*5,minecraft:andesite*3,minecraft:gravel*2
blockset.watercourse.forest.wet_bank=minecraft:moss_block*3,minecraft:rooted_dirt*2,minecraft:mud
blockset.watercourse.dryland.dry_bank=minecraft:sand*5,minecraft:red_sand*2,minecraft:smooth_sandstone
```

Für jedes Profil stehen vier Endungen zur Verfügung:

| Endung | Bedeutung |
| --- | --- |
| `.bed` | überfluteter Gewässergrund |
| `.wet_bank` | innere Feuchtzone |
| `.dry_bank` | äußerer trockener Übergang |
| `.bank_filler` | tragender Untergrund unter beiden Uferzonen |

Ein Eintrag ohne `*Gewicht` hat Gewicht 1. Zulässig sind Gewichte von 1 bis 64; die Summe eines
Blocksets darf 256 nicht überschreiten.

Ozeanböden besitzen eigene physische Formationen. Sie werden nach Wassertiefe, Hangneigung und
Temperatur gewählt und können ebenfalls überschrieben werden:

```properties
blockset.ocean.shallow_warm=minecraft:sand*5,minecraft:gravel*2,minecraft:clay
blockset.ocean.shallow_cold=minecraft:gravel*3,minecraft:stone,minecraft:sand,minecraft:clay
blockset.ocean.shelf=minecraft:sand*2,minecraft:gravel*3,minecraft:clay*2
blockset.ocean.deep=minecraft:clay*3,minecraft:gravel*2,minecraft:mud,minecraft:stone
blockset.ocean.rocky=minecraft:stone*3,minecraft:andesite*2,minecraft:gravel
```

Der globale Schlüssel `blockset.ocean_bed` bleibt ein bewusster Komplett-Override und ersetzt bei
Angabe alle fünf Standardformationen.

Schmale, vollständig von einem zusammenhängenden Fluss- oder Seespiegel eingeschlossene
Landspalten können abschließend repariert werden:

```properties
hydrology.gap_repair_radius=2
```

Der Standardwert 2 erfasst auch die auf älteren Screens sichtbaren zwei Block breiten Inseln. Die
Reparatur verlangt Wasserbelege auf gegenüberliegenden Seiten, mindestens vier nasse Nachbarpunkte
und höchstens einen Block Höhendifferenz. Offene Ufer, Küsten und beliebiges niedriges Gelände
werden dadurch nicht geflutet. `0` deaktiviert nur diese abgeleitete Reparatur; echte von der Engine
ausgewiesene Wasserspalten werden weiterhin wiederhergestellt. Im Tiefland werden reparierte
Spalten mit drei vollen Wasserblöcken ausgeformt, oberhalb davon mit zwei.

## Nachgelagerte natürliche Dekoration

r31 führt den Materializer-Hook nach den nativen Biomfeatures aus. Der Standardmaterializer setzt
nur Elemente, deren Platzierungsbedingungen im fertigen Chunk erfüllt sind:

| Element | Habitat/Begrenzung |
| --- | --- |
| Seegras / hohes Seegras | vorhandenes Vanilla-Seegras wird auf breite Habitatcluster in 2–8 Block tiefem, nicht zu steilem Wasser ausgedünnt und dort gezielt ergänzt |
| Seerosen | feuchte Seen mit freier Wasseroberfläche |
| Moosteppich | feuchte innere Uferzone; nie als tragender Bettblock |
| Zuckerrohr, Farn, Gras, Bambus | klima- und feuchteabhängige, zusammenhängend verteilte Uferpunkte |
| Gras, Farn und heimische Blüten | optionale breite Landhabitate passend zur semantischen Biomrolle; kein blockweises Weißrauschen |
| Wassergefüllte Stein-/Sandsteinstufen | seltene zusammenhängende Bettformationen in steilerem Gelände |
| Gischt | nur an einem nachgewiesenen Einblock-Gefälleschritt eines Hochlandflusses; Spinnwebe über Wasser und weißer Teppich auf einem trockenen Vorsprung |
| kleiner Damm | sehr selten, quer zu einem 3–8 Block breiten gemäßigten Fluss; Eichenstämme, Zaun und Schlammenden |

Weißes oder schwarzes Betonpulver wird absichtlich nicht an bzw. unter Wasser gesetzt, weil es dort
zu Beton aushärtet. Jede Platzierung ist auf den aktuell bearbeiteten Chunk beschränkt; damit
entstehen an Chunkgrenzen keine halben Doppelblockpflanzen oder abgeschnittenen Dämme.

```properties
decoration.enabled=true
decoration.plants=true
decoration.land_plants=true
decoration.partial_blocks=true
decoration.spray=true
decoration.dams=true
```

## Strikte Versionsbindung

Der Dekorator liegt ausschließlich in `families/mc1201`. Für Minecraft 1.20.1 wurden nur dort
vorhandene Block- und State-Eigenschaften verwendet: `SEAGRASS`, `TALL_SEAGRASS`, `LILY_PAD`,
`MOSS_CARPET`, `SUGAR_CANE`, `FERN`, `GRASS`, `BAMBOO`, `COBBLESTONE_STAIRS`,
`DANDELION`, `POPPY`, `AZURE_BLUET`, `CORNFLOWER`, `ANDESITE_STAIRS`, `SANDSTONE_STAIRS`,
`COBWEB`, `WHITE_CARPET`, `OAK_LOG`, `OAK_FENCE`, `MUD`
sowie `WATERLOGGED`, `HORIZONTAL_FACING`, `AXIS` und `DOUBLE_BLOCK_HALF`. Spätere
Minecraft-Familien müssen ihre eigene Verfügbarkeitsliste und eigene Dekoratorimplementierung
bereitstellen; r31 kopiert diese Auswahl nicht in die Platzhalter der Versionsmatrix.

## Tiefenprofil des Begleit-Engine-Stands

FlTerraForged r31 ist für Engine r29 vorgesehen:

- Tiefland-Flusskerne: Zielwert etwa 3,5 Blöcke vor dem Ufer-Taper;
- mittlere Lagen: weich abnehmend bis etwa 2,75 Blöcke;
- Hochland: weich abnehmend auf etwa 2,25 Blöcke;
- extremes Hochland: langfristig bis etwa 1,75 Blöcke;
- große, niedrige Seen: etwa 3–4 Blöcke im zusammenhängenden Innenkörper und kontinuierlich bis
  ungefähr 10–14 Blöcke in ausreichend großen/tiefen Kernen;
- kleine Einzelbecken/Teiche dürfen weiterhin flach bleiben.

Alle Höhen sind kontinuierliche Engine-Zielwerte. Der mc1201-Standardmaterializer quantisiert sie
anschließend konsistent auf ganze Minecraft-Blöcke.

Engine r29 berechnet zusätzlich ein kontinuierliches, basin-eigenes Distanzfeld zur Seeuferkante.
Damit kann die Tiefe an einer internen Drainage-Rastergrenze nicht mehr springen. Der finale
Hydrologiepass senkt eingeschlossene Resthügel auf den nachbarschaftlich belegten Bettverlauf ab;
er hebt dabei keine natürlichen Vertiefungen an und führt keinen freien Flood-Fill aus.

Bei Flussmündungen wird die Sohltiefe nicht mehr aus Breite oder Durchfluss des jeweils nächsten
Segments abgeleitet. Ein Wechsel vom Neben- zum Hauptfluss kann deshalb keinen vertikalen Graben
erzeugen. Die Engine toleriert bis zu drei zusätzliche Blöcke eines lokalen Resthügels. Bei einem
tieferen projizierten Kreuzungssegment wählt sie zuerst das besser zur Geländeoberfläche passende
Segment; nicht materialisierbare Restpunkte werden als flache Wasserlinienbank statt als Bergzacke
ausgeformt und dürfen nur bei gegenüberliegendem Wassernachweis repariert werden.

Der abschließende r29-Kantenaudit tastete für zwei unabhängige Seeds Fluss-, See- und
Ozeanübergänge gemeinsam ab. Unter 3.173 direkten Wasser-zu-Land-Kanten gab es keine trockene
Spalte unterhalb der Wasserlinie und keine erste Uferstufe von mehr als einem Block.
