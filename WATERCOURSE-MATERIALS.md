# Standardmaterialisierung für Wasserläufe

Stand: **0.1.0-SNAPSHOT-r29**, Minecraft-Familie **mc1201**

## Ziel

Der Standardmaterializer bildet Flüsse, Seen und ihre Ufer nicht mehr mit je einem einzigen Block
ab. Jede Oberflächen-Wasserstelle wird zunächst einem Höhen-/Klimaprofil und anschließend einer von
drei sichtbaren Zonen zugeordnet:

1. **Nasszone:** der feste, vollständig überflutete Gewässergrund;
2. **Feuchtzone:** die innere Uferzone an der Wasserlinie;
3. **Trockenzone:** der äußere Übergang in die normale Biomoberfläche.

Unter Feucht- und Trockenzone liegt zusätzlich ein profilspezifischer stabiler Füller. Die Auswahl
innerhalb einer Palette erfolgt deterministisch in kleinen zusammenhängenden Flecken. Sie ist daher
reproduzierbar und erzeugt kein unabhängiges Zufallsrauschen pro Block.

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

## Bewusst noch nicht als tragendes Material verwendet

Folgende gewünschte Elemente gehören nicht in den Full-Block-Untergrund und werden daher nicht
blind als Bettblock gesetzt:

- Stufen, Teppiche und Schneeschichten benötigen Teilblock-/Waterlogging-Unterstützung;
- Seegras, Seerosen, Bambus, Ranken, Tropfblatt und Sporenblüten benötigen Platzierungs- und
  Überlebensregeln sowie eine kontrollierte Dichte;
- Zäune als Biberdamm sind ein Geländeelement und kein zufälliges Ufermaterial;
- weißes Betonpulver härtet an Wasser aus; Spinnweben benötigen eine echte Wasserfall-/Gischtzone;
- schwarzes Betonpulver ist unter Wasser nicht stabil und wird deshalb durch Schlamm/Lehm ersetzt;
- Leuchtbeeren können nicht einfach als Unterwasser-Bettblock verwendet werden.

Diese Elemente gehören in einen nachgelagerten, materializer-gesteuerten Dekorationspass. Die hier
eingeführte Zonierung und positionsabhängige SPI bilden dafür die Grundlage, ohne die derzeitige
Full-Block-Geometrie oder externe Materializer zu umgehen.

## Tiefenprofil des Begleit-Engine-Stands

FlTerraForged r29 ist für Engine r27 vorgesehen:

- Tiefland-Flusskerne: Zielwert etwa 3,5 Blöcke vor dem Ufer-Taper;
- mittlere Lagen: weich abnehmend bis etwa 2,75 Blöcke;
- Hochland: weich abnehmend auf etwa 2,25 Blöcke;
- extremes Hochland: langfristig bis etwa 1,75 Blöcke;
- große, niedrige Seen: mindestens etwa 3,5 Blöcke schon im zusammenhängenden Wasserkörper und
  deutlich tiefere Kerne;
- kleine Einzelbecken/Teiche dürfen weiterhin flach bleiben.

Alle Höhen sind kontinuierliche Engine-Zielwerte. Der mc1201-Standardmaterializer quantisiert sie
anschließend konsistent auf ganze Minecraft-Blöcke.
