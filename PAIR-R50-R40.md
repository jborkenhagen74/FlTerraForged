# FlTerraForged R50 / Engine R40 pair

This branch is intended to be built and tested only with FlTerraForged Engine R40.

## Pairing

- FlTerraForged: R50 (`revision/r50-receiver-overlay`)
- FlTerraForged Engine: R40 (`revision/r40-receiver-overlay`)
- Minecraft reference binding: 1.20.1 Fabric

The CI workflow builds the local R50 Engine API, builds Engine R40 against that API, and then runs the complete R50 `clean check` against the locally built R40 repository before collecting the installable Fabric JAR.

## Hydrology contract

R50 expects Engine R40 to provide final receiver-owned continuous geometry. Ocean/lake filling is not reconstructed after Minecraft carving. Variable-height materializers continue to receive the same resolved Engine semantics and may expose their realized surface geometry through the existing provider SPI.
