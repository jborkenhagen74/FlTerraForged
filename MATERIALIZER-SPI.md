# Austauschbare Block-Materializer – Minecraft 1.20.1 / Fabric

Stand: **0.1.0-SNAPSHOT-r28**

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

Die Materialisierung ist in r26 nicht mehr nur ein Höhen-Quantizer. Der aktive Materializer liefert
konsistent für alle relevanten Worldgen-Stufen:

- vertikale Auflösung und Capabilities,
- materialisierte Festbodenhöhe,
- materialisierte Wasseroberkante,
- Entscheidung, ob Engine-Hydrologie tatsächlich Wasser erzeugt,
- Bedrock/Floor,
- neu erzeugtes Substrat,
- Surface-Seal vor Carvern,
- Luft und Fluid,
- deterministischen Top-/Filler-State für synchrone Column-Samples,
- erzwungene Surface-Overrides,
- Surface-Fallback,
- Hydrologie-Bett,
- Hydrologie-Seal nach Cave-Carving.

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

The built-in materializer accepts optional comma-separated block-id sets. Examples:

```properties
blockset.river_bed=minecraft:gravel,minecraft:cobblestone
blockset.lake_bed=minecraft:gravel,minecraft:clay
blockset.plains=minecraft:grass_block
blockset.valley=minecraft:grass_block
blockset.hills=minecraft:grass_block,minecraft:stone
blockset.plateau=minecraft:grass_block,minecraft:stone
blockset.mountains=minecraft:stone,minecraft:gravel
blockset.ocean_bed=minecraft:gravel,minecraft:sand
```

If an option is absent, the built-in full-block behavior remains unchanged. Selection inside a configured set is deterministic from the Engine sample, so chunk regeneration does not reshuffle materials. An external materializer may reuse these keys or ignore them entirely.

`mayRepairHydrologyGap(...)` and `hydrologyGapBedY(...)` are also materializer hooks. This keeps final water-gap repair compatible with future partial-block/waterlogging materializers.
