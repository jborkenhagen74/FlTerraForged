# Contributing

## Code placement rule

Bei einem Minecraft-Versionsbruch gilt zwingend diese Reihenfolge:

1. Kann die Änderung in `common` gelöst werden?
2. Ist sie loader-spezifisch und gehört in `platforms/<loader>/common`?
3. Betrifft sie mehrere benachbarte Minecraft-Versionen? Dann gehört sie in eine `family`.
4. Betrifft sie nur eine Loader-/Family-Kombination? Dann in `families/<family>/<loader>`.
5. Nur wenn exakt eine Minecraft-Version abweicht, ist Code unter `versions/<version>/...` zulässig.

Vollständige Kopien eines Source Trees pro Minecraft-Version sind nicht zulässig.

## Engine API rule

`engine-api` darf keine Minecraft- oder Loader-Abhängigkeiten erhalten.

## Upstream provenance

Wenn Code aus TerraForged, ReTerraForged, FreeTerraForged oder AronaLayers
übernommen oder stark abgeleitet wird:

- Lizenz prüfen;
- Copyright-Hinweis erhalten;
- Herkunft in `UPSTREAMS.md` ergänzen;
- bei größeren Übernahmen zusätzlich im Source Header dokumentieren.


## Java versions for development

The Minecraft 1.20.1 adapter targets Java 17 bytecode, but the current Fabric Loom 1.17.x plugin must be loaded by Gradle on Java 21. Use Java 21 to run Gradle and keep a Java 17 JDK installed for Gradle toolchains. CI enforces this split explicitly.
