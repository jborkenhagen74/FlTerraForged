# conquest

Conquest Reforged wird später über Registry-/Tag-basierte Surface- und Decoration-Provider angebunden.


## r25 materialization hook

The mc1201 host now exposes `TerrainMaterializer` as the vertical-resolution boundary. Vanilla uses
`VanillaTerrainMaterializer` with 1.0-block resolution. A future Conquest integration should provide a
materializer that advertises 0.5/0.25-block support and waterlogging where the selected Conquest
states permit it. Basin water levels remain Engine-owned and must not be recomputed by this layer.
