# FlTerraForged R51 / Engine R41 pair

This branch is the startup-liveness successor to the R50/R40 receiver-overlay pair and is intended to be built and tested only with the pinned Engine R41 revision.

## Pairing

- FlTerraForged: R51 (`revision/r51-startup-liveness`)
- FlTerraForged Engine: R41 (`revision/r41-startup-liveness`)
- Minecraft reference binding: 1.20.1 Fabric

The CI workflow builds the local FlTerraForged Engine API, checks out exactly the pinned Engine R41 commit, builds Engine R41 against that API, and then runs the complete R51 `clean check` against the locally built Engine repository before collecting the installable Fabric JAR.

## Startup liveness contract

R51/R41 treats cold world creation as a correctness requirement rather than only a performance target. R40's broad rivermap planning accidentally routed every padded drainage probe through hydraulic erosion. R41 separates those responsibilities again: coarse drainage topology and path planning use deterministic pre-erosion base terrain, final local river incision still uses post-erosion terrain, and the receiver overlay remains authoritative after river shaping.

Expensive canonical datasets use bounded synchronous single-flight caching. Erosion regions, immutable rivermaps and final terrain tiles are generated once per cold key in Engine R41; R51 applies the same rule to marine structure columns and ring summaries. The winning caller computes inline, no cache loader submits work to Minecraft's world-generation executor, and completed-cache monitors are never held during generation.

The cache dependency graph remains acyclic. Marine summaries depend on columns, columns depend on `TerrainWorld.environment`, Engine hydrology depends on rivermaps, and final local hydrology may depend on erosion. No lower layer calls back into marine structure summaries or Minecraft chunk generation.

## Receiver and materializer contract

The R40 receiver-owned continuous geometry remains active: river shaping and wet-core continuity run before the receiver overlay, which resolves final lake/ocean ownership without a Minecraft-side refill or terrain reconstruction pass. Variable-height materializers continue to receive the same Engine semantics through the existing provider SPI.

R51 retains the Engine-only `session.bind(...)` path during `STRUCTURE_STARTS`; it must not bind the full Engine-backed BiomeSource there. Marine structure eligibility still uses the same physical environment semantics while avoiding the previously isolated startup re-entry path.
