# FlTerraForged R49 / Engine R39 Paarstand

Dieser Stand koppelt FlTerraForged R49 für Minecraft 1.20.1 fest mit FlTerraForged Engine R39.

## Verbindlicher Engine-Stand

- Repository: `jborkenhagen74/FlTerraForged-Engine`
- Revision: `R39`
- Branch: `revision/r39-receiver-continuity`
- Runtime-Code-Commit: `57b5129cdcc33229269381836c46ffa8c69dfd67`

Der R49-Build checkt exakt diesen Engine-Commit aus. Damit kann ein späterer Commit auf dem Engine-Branch den geprüften Paarstand nicht stillschweigend verändern.

## Enthaltene Korrekturen

- kanonische Near-Integer-Höhenquantisierung vor der Minecraft-Blockprojektion,
- garantierte vollständige Wasserzelle für semantisch nasse Full-Block-Spalten,
- semantischer Nass-Schutz bereits vor dem Carving,
- R48-Unterwasserboden- und Seitensiegel bleiben aktiv,
- kein mutierender Wasser-Reparaturpass nach der Generierung,
- trockene Lake-Shore-Übergänge bleiben trocken,
- Seen und offenes Meer übernehmen an bestätigten Mündungen den Wasserpegel,
- bestätigte Empfangsgewässer übernehmen zusätzlich die Mündungssohle, sodass der künstlich tiefere Flussgraben nicht als Rinne durch See oder Meer fortgeführt wird,
- echte terrainbedingte Wasserfälle bleiben erhalten.

## Validierung

Der Paarstand gilt erst als Teststand, wenn der R49-Workflow vollständig erfolgreich ist. Der Workflow baut zunächst die R49 Engine API, anschließend exakt Engine R39 aus dem oben angegebenen Commit und führt danach `clean check` inklusive der vorhandenen Verifier und Javadoc-Prüfungen aus. Erst danach wird das installierbare Fabric-1.20.1-JAR veröffentlicht.
