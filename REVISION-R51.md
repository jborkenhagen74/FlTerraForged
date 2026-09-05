# FlTerraForged R51

R51 is the Minecraft 1.20.1 host-side startup-liveness correction paired with FlTerraForged Engine R41.

## Runtime regression addressed

The R50/R40 pair could build successfully while a newly created world remained at `0%` for minutes. The primary amplification was inside Engine R40: padded coarse drainage planning had been switched from base terrain to the hydraulic erosion lookup. R51 therefore requires the R41 Engine liveness contract and adds host-side duplicate-work prevention around marine structure checks.

## Marine environment cache

The previous `MarineEnvironmentCache` intentionally used optimistic completed-value caches. A cold miss was generated outside the cache monitor and a second lookup selected the retained value. That prevented monitor-held generation, but concurrent callers could compute the same Engine environment column or ring multiple times.

R51 replaces both levels with `WorldgenSingleFlightCache`:

- one owner computes each cold key inline on its existing world-generation thread;
- callers for the same key reuse the same future instead of duplicating work;
- no cache loader submits another world-generation task;
- no completed-cache monitor is held while the Engine is sampled;
- same-thread recursion for an already owned key fails immediately instead of self-waiting;
- only completed values enter the bounded LRU;
- failed loads are removed from in-flight ownership and are not retained as completed data.

The host dependency graph is deliberately acyclic: `ring -> column -> TerrainWorld.environment`. Column resolution never calls a ring or Minecraft chunk generation.

## Structure-start lifecycle

R51 preserves the Engine-only binding introduced to protect structure-start discovery:

`placementWorld = session.bind(placementCalculator.getNoiseConfig())`

The marine guard must not call the generator's full `bind(...)` method during `STRUCTURE_STARTS`, because that also binds the Engine-backed `BiomeSource` while vanilla is still discovering starts.

## Inherited worldgen behavior

R51 retains the R50 materializer boundary, hydraulic height quantization, owned-carver lifecycle, receiver-owned ocean/lake geometry and the absence of any post-generation water refill or terrain reconstruction pass.

Runtime liveness is validated primarily by the paired Engine R41 cold-start tests; `tools/verify-r51-startup-liveness.py` additionally locks down the host-side binding and single-flight invariants during normal CI.
