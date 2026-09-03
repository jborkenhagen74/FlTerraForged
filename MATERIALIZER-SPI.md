# Austauschbare Block-Materializer – Minecraft 1.20.1 / Fabric

Stand: **0.1.0-SNAPSHOT-r34**

## Ziel

Die Engine beschreibt ausschließlich kontinuierliche Weltgeometrie und Semantik. Sie entscheidet
nicht, welcher Minecraft-Block gesetzt wird. FlTerraForged bildet diese Semantik über einen
`BlockMaterializer` auf die konkrete Minecraft-Version ab.

Der Standardmaterializer wird mit FlTerraForged ausgeliefert. Drittanbieter können einen eigenen
Materializer als **separate Fabric-Mod** bereitstellen. FlTerraForged entdeckt diese Provider beim
Start und verwendet ausschließlich den in der Konfiguration ausgewählten Provider.

```text
FlTerraForged Engine
  TerrainSample / Climate / Hydrology
                |
                v
FlTerraForged mc1201
  MaterializerRegistry
      |         |         |
      |         |         +-- thirdparty:custom
      |         +------------ conquestaddon:conquest
      +---------------------- flterraforged:vanilla
                |
          configuration
                |
                v
       ausgewählter Provider
                |
                v
        BlockMaterializer
                |
   +------------+-------------+-------------+
   |            |             |             |
 Density     Columns       Surface       Carver
 Bridge      / Height       Guard         Guard
```

## Standardkonfiguration

Beim ersten Start erzeugt die Fabric-Anbindung:

```text
config/flterraforged/materializer.properties
```

Standardinhalt:

```properties
materializer=flterraforged:vanilla
```

Ein externes Add-on kann beispielsweise registrieren:

```properties
materializer=conquestaddon:conquest
```

Die Auswahl wird einmal beim Mod-Start geladen. Nach einer Änderung ist ein Neustart erforderlich.
Ein Materializer-Wechsel beeinflusst neu erzeugte Chunks und sollte deshalb in bestehenden Welten nur
bewusst vorgenommen werden.

Wenn eine konfigurierte Provider-ID nicht registriert ist, startet FlTerraForged **nicht** mit einem
stillen Fallback. Das verhindert, dass nach Entfernen einer Add-on-Mod unbemerkt andere Blockgeometrie
in neuen Chunks entsteht.

## Öffentliche SPI

Die 1.20.1-SPI liegt unter:

```text
dev.foucaultleon.flterraforged.api.mc1201.materializer
```

Wichtige Typen:

- `BlockMaterializer`
- `BlockMaterializerProvider`
- `MaterializerContext`
- `MaterializerCapabilities`
- `MaterializerRegistry`
- `DelegatingBlockMaterializer`
- `WaterDecorationContext`

Die SPI ist bewusst **Minecraft-familienbezogen**. Ein Materializer für 1.20.1 muss nicht binär mit
einer späteren Minecraft-Familie kompatibel sein. Die Engine-API bleibt davon unberührt und weiterhin
Minecraft-unabhängig.

## Provider implementieren

Ein Add-on registriert einen Provider:

```java
public final class ConquestMaterializerProvider implements BlockMaterializerProvider {
    @Override
    public String id() {
        return "conquestaddon:conquest";
    }

    @Override
    public BlockMaterializer create(MaterializerContext context) {
        return new ConquestMaterializer(context);
    }
}
```

Die Provider-ID muss kleingeschrieben und namespaced sein.

## Fabric-Entrypoint

Im `fabric.mod.json` der Drittanbieter-Mod:

```json
{
  "entrypoints": {
    "flterraforged:materializer": [
      "example.ConquestMaterializerProvider"
    ]
  },
  "depends": {
    "minecraft": "=1.20.1",
    "flterraforged": "*"
  }
}
```

FlTerraForged liest **alle** Provider dieses Entrypoints. Doppelte IDs gelten als Konfigurationsfehler
und führen zum Startabbruch.

## Vollständiger Ersatz oder Decorator

Ein eigener Materializer kann `BlockMaterializer` vollständig implementieren. Für Add-ons, die nur
einzelne Materialentscheidungen ändern möchten, gibt es `DelegatingBlockMaterializer`.

Beispiel: nur Fluss-/Seegrund ersetzen:

```java
public final class CustomMaterializer extends DelegatingBlockMaterializer {
    public CustomMaterializer(BlockMaterializer delegate) {
        super(delegate);
    }

    @Override
    public BlockState hydrologyBedState(TerrainSample sample) {
        return MyBlocks.CUSTOM_RIVERBED.getDefaultState();
    }
}
```

Der Provider kann dabei den mit FlTerraForged gelieferten
`VanillaBlockMaterializer` als Delegate verwenden.

## Was der Materializer kontrolliert

Die Materialisierung ist seit r26 nicht mehr nur ein Höhen-Quantizer. Der aktive Materializer
liefert konsistent für alle relevanten Worldgen-Stufen:

- vertikale Auflösung und Capabilities,
- materialisierte Festbodenhöhe,
- materialisierte Wasseroberkante,
- Entscheidung, ob Engine-Hydrologie tatsächlich Wasser erzeugt,
- Bedrock/Floor,
- neu erzeugtes Substrat,
- Surface-Seal vor Carvern,
- Luft und Fluid,
- deterministischen Top-/Filler-State für synchrone Column-Samples,
- positionsabhängige Top-/Filler-/Ufer-/Gewässerbett-Auswahl für räumlich kohärente Paletten,
- erzwungene Surface-Overrides,
- Surface-Fallback,
- Hydrologie-Bett,
- Hydrologie-Seal nach Cave-Carving,
- optionalen versionsgebundenen Bewuchs, Teilblöcke, Gischt und kleine Dämme nach nativen Features.

Damit kann eine externe Mod die Standardmaterialisierung tatsächlich ersetzen. Es bleiben keine
fest verdrahteten `Blocks.GRAVEL`, `Blocks.SAND`, `Blocks.DIRT` usw. in der Worldgen-Pipeline, die den
Provider später wieder überschreiben.

## Conquest Reforged

Eine spätere Conquest-Mod kann z. B. melden:

```text
verticalResolution = 0.25
partialBlocks       = true
waterlogging        = true/abhängig von den verwendeten States
```

Die Engine berechnet weiterhin kontinuierliche Werte wie `surfaceHeight=81.25`. Erst der
Conquest-Materializer entscheidet, welcher Full-/Half-/Quarter-State diese Höhe repräsentiert.
Seen-/Flusspegel bleiben Engine-eigen; der Materializer darf sie nur auf die verfügbare
Minecraft-Blockauflösung abbilden.

## Beispiel

Unter `examples/materializer-addon` liegt ein bewusst nicht in den Root-Build eingebundenes
Referenz-Add-on. Es zeigt Provider-Entrypoint, Konfigurations-ID und einen Decorator, der nur das
Hydrologie-Bett ersetzt.


## Standardmaterializer: Blocksets

The built-in materializer accepts optional comma-separated block-id sets. Entries may carry a
weight suffix (`*1` through `*64`). Examples:

```properties
blockset.river_bed=minecraft:gravel*5,minecraft:cobblestone*2
blockset.lake_bed=minecraft:gravel*2,minecraft:clay*4
blockset.plains=minecraft:grass_block
blockset.valley=minecraft:grass_block
blockset.hills=minecraft:grass_block,minecraft:stone
blockset.plateau=minecraft:grass_block,minecraft:stone
blockset.mountains=minecraft:stone,minecraft:gravel
blockset.ocean_bed=minecraft:gravel,minecraft:sand
```

If a global option is absent, r34 uses the complete height- and climate-aware three-zone
watercourse palette. Profile-specific keys use the form
`blockset.watercourse.<profile>.<bed|wet_bank|dry_bank|bank_filler>`. See
`WATERCOURSE-MATERIALS.md` for the complete mapping. Selection follows a deterministic,
domain-warped low-frequency field, so chunk regeneration does not reshuffle materials and adjacent
surfaces form geological bands instead of fixed-size random patches. An external materializer may
reuse these keys or ignore them entirely.

The original sample-only SPI methods remain available. Position-aware overloads are additive and
default to their original counterpart, so an existing provider still compiles and behaves as
before. A provider that wants spatially varying decisions can override the new overloads.

`mayRepairHydrologyGap(...)`, `hydrologyGapBedY(...)` and
`hydrologyGapRepairRadius()` are also materializer hooks. The standard provider uses radius 2 and
requires consistent wet evidence on opposing sides; external providers may reduce the radius to
zero or replace the bed quantization. This keeps final water-gap repair compatible with future
partial-block/waterlogging materializers.

`decorateWatercourses(WaterDecorationContext)` is an additive default no-op hook. The host calls it
after native biome features for the current chunk. Existing providers therefore stay source
compatible. A provider overriding it owns every concrete block and must enforce the availability
and block-state rules of its Minecraft family. `DelegatingBlockMaterializer` forwards the hook.
