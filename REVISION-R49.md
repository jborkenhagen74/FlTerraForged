# FlTerraForged R49

R49 builds on R48 and fixes block-height quantization at ocean, river and lake transitions without adding a mutating post-generation water repair pass.

## Stable height quantization

- Near-integer Engine heights are snapped within `1.0E-6` before conversion to Minecraft block coordinates.
- The standard full-block materializer uses the same canonical conversion for terrain and hydrology water levels.
- A mathematically integral level represented as e.g. `62.999999999` can no longer lose an entire Minecraft water layer through a raw `Math.floor` call.
- Ocean columns classified as physically submerged reserve at least one full water cell in the standard full-block provider.
- Any continuous Engine river/lake water column reserves at least one complete fluid cell when represented by the standard full-block provider.

## Carver protection

R48 protected the ocean/lake floor only after `hasFinalWetEnvelope(...)` was already true at block resolution. R49 additionally derives a semantic wet expectation from the continuous Engine sample. Therefore a quantization edge cannot simultaneously make a column appear dry and remove its underwater carve ceiling.

The existing R48 depth-aware ocean/lake floor seals and lateral wet-neighbor seals remain active. No post-carver bed reconstruction pass is introduced.

## Provider boundary

Variable-height providers keep exact authority over their `MaterializedSurfaceGeometry`, fluid states, waterlogging and final wet states. The new canonical quantizer is used by the standard full-block implementation and legacy/full-block geometry fallback; custom geometry providers are not rounded to full blocks by the host.

## Engine pairing

R49 is validated against FlTerraForged Engine R39. R39 extends receiving-lake authority across narrow, corroborated wet-core connectors, keeps dry lake-shore transitions dry, uses bounded cached lake-only probes and preserves genuine terrain-backed waterfalls.
