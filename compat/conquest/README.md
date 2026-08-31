# Conquest Reforged compatibility

Conquest Reforged should be integrated as a **separate materializer add-on mod**, not as an Engine dependency.

Since r26 the mc1201 host exposes the public `BlockMaterializer` SPI. FlTerraForged ships `flterraforged:vanilla`; a Conquest add-on can register e.g. `flterraforged_conquest:conquest` through the `flterraforged:materializer` Fabric entrypoint and the user selects it in `config/flterraforged/materializer.properties`.

The Conquest provider may advertise 0.5/0.25-block vertical resolution, partial blocks and waterlogging and may replace surface/filler/riverbed/seal states. Basin and river water levels remain Engine-owned and must not be recomputed by the add-on. See `MATERIALIZER-SPI.md`.
