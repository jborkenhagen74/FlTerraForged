# FlTerraForged R50

R50 is the Minecraft 1.20.1 reference binding for FlTerraForged Engine R40.

The Minecraft-side R49 quantization, semantic wet protection, R48 submerged-floor seal and variable-height materializer geometry remain unchanged. The hydrology fix is intentionally performed in Engine space before materialization rather than by another chunk repair pass.

## Paired Engine semantics

Engine R40 resolves final geometry in the order:

`terrain -> erosion -> river -> wet-core continuity -> receiving-water overlay`

The receiver overlay ensures that open ocean and material lake regions own their final bed and water level after the river has been shaped. FlTerraForged then materializes that already-consistent continuous geometry exactly once.

## Minecraft lifecycle

R50 retains the existing lifecycle:

1. vanilla 3D substrate generation;
2. Engine-density reshape/materialization;
3. vanilla surface rules plus Engine surface guard;
4. FlTerraForged-owned carver with protected wet floors;
5. features and watercourse/shore decoration.

There is no post-carver sea/lake refill or post-generation terrain reconstruction pass.
