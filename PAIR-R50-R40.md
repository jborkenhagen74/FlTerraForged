# FlTerraForged R50 / Engine R40 pair

This branch is intended to be built and tested only with the pinned FlTerraForged Engine R40 revision below.

## Pairing

- FlTerraForged: R50 (`revision/r50-receiver-overlay`)
- FlTerraForged Engine: R40 (`revision/r40-receiver-overlay`)
- Engine R40 commit: `c3b0655a1893023c5dc69eb7a4ebd8dfbcc1913c`
- Minecraft reference binding: 1.20.1 Fabric

The CI workflow builds the local R50 Engine API, checks out exactly the pinned Engine R40 commit, builds Engine R40 against that API, and then runs the complete R50 `clean check` against the locally built R40 repository before collecting the installable Fabric JAR.

## Hydrology contract

R50 expects Engine R40 to provide final receiver-owned continuous geometry. Drainage topology and river incision use the same post-erosion terrain. River shaping and wet-core continuity run before the final receiver overlay, which restores ocean- and lake-owned geometry from the preserved pre-hydrology surface. Ocean/lake filling is not reconstructed after Minecraft carving.

Variable-height materializers continue to receive the same resolved Engine semantics and may expose their realized surface geometry through the existing provider SPI. The receiver overlay does not re-run terrain or erosion sampling and there is no post-generation water repair pass.
