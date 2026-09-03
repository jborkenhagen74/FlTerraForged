## 0.1.0-SNAPSHOT-r34

- Pair with Engine `0.1.0-SNAPSHOT-r30`, which coalesces concurrent cold misses for identical
  terrain tiles, erosion regions and river maps instead of multiplying work across spawn workers.
- Prevent empty vanilla structure starts and non-marine structures from triggering any Engine
  footprint samples.
- Reject inland marine candidates after the center sample and replace the cold 25-point survey with
  one center plus at most eight perimeter points.
- Avoid rewriting the chunk structure-start map when the guard retained every start.
- Add regression limits for zero-sample irrelevant starts, one-sample inland rejection and the
  bounded nine-sample deep-marine acceptance path.
- Treat the r33 0-percent world-creation stall as a release blocker; waterfall/rapid work remains
  deferred until this hotfix passes real Minecraft 1.20.1 startup and exploration tests.

## 0.1.0-SNAPSHOT-r33

- Correct the Minecraft-1.20.1 Yarn import for `StructureSet` from the invalid
  `net.minecraft.world.gen.structure.StructureSet` package to `net.minecraft.structure.StructureSet`.
- Preserve the r32 marine-structure placement guard unchanged and continue pairing with Engine r29.

## 0.1.0-SNAPSHOT-r32

- Add a Minecraft-1.20.1 marine-structure guard at `STRUCTURE_STARTS`, after vanilla has selected
  candidates but before invalid starts are retained in the chunk.
- Require shipwrecks, cold/warm ocean ruins and monuments to have a connected 64-by-64-block
  ocean/coast footprint instead of accepting a nearby ocean biome as sufficient evidence.
- Reject marine starts when any footprint sample is river, lake, pond, dry terrain or shallower
  than two blocks; additionally require structure-specific center depths of 5, 6 or 12 blocks.
- Leave non-marine structures and vanilla's separate beached-shipwreck structure unchanged.
- Add strict regression coverage for deep ocean acceptance, river/lake/puddle rejection, mixed
  ocean/river footprints, monument depth and non-marine pass-through behavior.
- Continue pairing with Engine r29; this is a Minecraft-family placement correction and does not
  change the version-neutral Engine API or hydrology geometry.

## 0.1.0-SNAPSHOT-r31

- Pair with Engine `0.1.0-SNAPSHOT-r29`, whose wet-to-dry river/lake shaping and coastal-height release remove first-bank cliffs, dry under-waterline cells and shore detail spikes.
- Permit bounded final gap repair inside the geometric dry channel envelope even when the corrected Engine classifier intentionally preserves the surrounding land/coast biome.
- Add broad seed-dependent irregular biome-variant stands shared by matrix families instead of deriving every native candidate from a narrow weighted climate interval.
- Rebalance both Central-Europe palettes toward mixed `forest` stands while retaining bounded birch, old-growth birch, spruce and dark-oak monoculture regions.
- Increase coherent moss-carpet and bank-plant coverage, add optional climate-role-driven land herb/flower clusters, and slightly enrich lake lily clusters without increasing uniform underwater grass.
- Restrict cobweb/carpet waterfall spray to actual river steps above Y 120; lowland lake and shore slopes can no longer trigger spray through operator precedence.
- Keep every concrete plant/block reference inside the Minecraft 1.20.1 family and add `decoration.land_plants` as an independent default-enabled materializer option.
- Keep the all-version build expansion deferred until r29/r31 passes real-world 1.20.1 validation.

## 0.1.0-SNAPSHOT-r30

- Replace fixed three-block material patches with continuous domain-warped formation fields for river, lake, shore and ocean sediments.
- Add depth-, slope- and climate-aware marine floor profiles for warm/cold shallows, continental shelf, deep floor and exposed rock; retain `blockset.ocean_bed` as a complete override.
- Blend the 8–12 block outer river/lake bank back into native biome surface blocks instead of forcing sediment across the whole transition.
- Add the optional `decorateWatercourses(WaterDecorationContext)` materializer hook after native features and forward it through `DelegatingBlockMaterializer`.
- Add the mc1201-only natural decorator for clustered seagrass/tall seagrass, lily pads, moisture-aware bank plants, moss carpets, waterlogged stairs, bounded waterfall spray and rare habitat-validated small dams.
- Thin native uniform seagrass into the same broad habitat clusters before adding missing plants; leave kelp and unrelated biome features untouched.
- Restrict every decorator write to the current chunk so double plants, spray and structures cannot be cut by cross-chunk writes.
- Smooth high residual hydrology bed cells only when opposing wet neighbors prove one continuous water surface; no free flood fill or raising of natural depressions.
- Pair with Engine `0.1.0-SNAPSHOT-r28`, whose continuous basin-boundary distance field and bounded river grade remove internal lake-bed cliffs and multi-block water breaks.
- Keep river-bed depth independent of the winning segment width/flow at confluences and allow only a bounded residual-ridge correction, eliminating the audited one-column hills without accepting deeply buried false water signals.
- Keep the all-version build expansion explicitly deferred until this 1.20.1 stability gate has been tested in a real world.

## 0.1.0-SNAPSHOT-r29

- Add a standard three-zone watercourse material system: submerged bed, damp waterline and dry outer transition, plus a stable bank filler below both shore zones.
- Select built-in full-block palettes from continuous height and climate/biome-role signals for high alpine, snowy highland, alpine, forest, dark forest, jungle, midland, plains, dryland, wetland, lush underground, rocky underground and deepslate profiles.
- Add position-aware materializer overloads without removing the original SPI methods. The standard provider uses deterministic three-block patches and quantized terrain signals instead of equal, positionless block selection.
- Extend configured block sets with optional `*weight` syntax and profile-specific `blockset.watercourse.<profile>.<zone>` overrides. Existing global river/lake/riparian keys retain priority.
- Expand every active riverbank, including river-classified dry edge columns, into an inner wet and outer dry transition before generic coast/desert surface handling.
- Expand the materializer-owned enclosed-gap repair from one to two blocks by default. Opposing wet evidence, a minimum neighborhood count and a consistent water level prevent the pass from flooding open banks; repaired lowland gaps receive three full water blocks.
- Pair with Engine `0.1.0-SNAPSHOT-r27`, whose altitude-aware river depth and basin-size-aware lake depth remove one-block-looking lowland water bodies while preserving shallow ponds.
- Document safe full-block substitutions and defer plants, partial blocks, waterfall spray and structure-like beaver dams to a later materializer-controlled decoration pass.

## 0.1.0-SNAPSHOT-r28

- Added depth-aware shallow/deep ocean biome routing for the 1.20.1 family, including deep-ocean palette roles that later Minecraft families can map to their expanded biome sets.
- Restored dry-climate riverbank priority over generic coast/desert surfaces so rivers crossing sand regions receive a vegetated riparian corridor again.
- Expanded the standard materializer river/lake cave-protection margin from 4 to 6 blocks and the bank seal depth from 6 to 8 blocks; both remain configurable and replaceable by third-party materializers.
- Marine fill repair now also restores submerged coast columns, reducing isolated holes along ocean and shoreline water surfaces. Sea-level filling is restricted to semantic ocean/coast columns instead of flooding arbitrary inland terrain below sea level.
- Updated the host to Engine r26, whose bathymetry and ocean/coast classification prevent ocean biomes and ocean structures from leaking onto dry land.

## 0.1.0-SNAPSHOT-r27


- Add matrix-ready, version-neutral `BiomeRole`/`BiomeRoleResolver` routing; each Minecraft family maps semantic roles to the native biomes available in that version.
- Replace the fixed mc1201 biome palette with a data-driven role -> candidate-list codec, allowing later Minecraft families to use additional biomes without changing Engine climate code.
- Add `central_europe` (randomized) and `central_europe_north_south` world presets; north-south climate is optional and independent from the terrain preset.
- Expand the 1.20.1 palette across cold/temperate oceans, taiga variants, birch/old-growth forests, flower forest, wetland, meadow and peak variants; remove windswept hills from the default mountain role.
- Add deterministic configurable standard-materializer block sets for river/lake beds, coasts, shores, riparian zones, plains, valleys, hills, plateaus, mountains, ocean bed, substrate and seals. Missing block-set options retain built-in full-block defaults.
- Add final hydrology connectivity repair for isolated one-column river/lake water gaps, while avoiding arbitrary flood fill.
- Increase default hydrology cave margin to 4 blocks and bank seal depth to 6 blocks.
- Pair with Engine `0.1.0-SNAPSHOT-r24`.

## 0.1.0-SNAPSHOT-r26

- Refactors Minecraft block realization into a replaceable `BlockMaterializer` SPI owned entirely by FlTerraForged; the external Engine remains unaware of Minecraft blocks and provider selection.
- Adds `BlockMaterializerProvider`, `MaterializerRegistry`, `MaterializerContext`, `MaterializerCapabilities` and `DelegatingBlockMaterializer` under the public mc1201 API package.
- Ships `flterraforged:vanilla` as the standard provider and moves all hard-coded vanilla block choices out of the geometry pipeline into `VanillaBlockMaterializer`.
- Fabric discovers third-party providers through the `flterraforged:materializer` entrypoint. Duplicate provider ids fail startup.
- Adds `config/flterraforged/materializer.properties`; `materializer=flterraforged:vanilla` is created by default and any registered external id can be selected. Missing configured providers fail fast instead of silently changing world generation.
- Density shaping, synchronous columns/height queries, surface correction and post-carver hydrology repair now delegate their concrete block/fluid choices to the selected materializer.
- Adds `MATERIALIZER-SPI.md` and a separate-mod example under `examples/materializer-addon`.
- Extends layout verification so concrete `Blocks.*` choices are forbidden in the mc1201 worldgen pipeline outside materializer implementations.

## 0.1.0-SNAPSHOT-r25

- Integrates Engine r22 basin-level inland water: connected ponds/lakes now use one flat spill-derived water surface instead of per-column interpolated lake levels.
- Adds additive `StandardTerrainTypes.LAKE_SHORE` semantics so dry shoreline transitions are no longer treated as wet gravel lake beds.
- Replaces the fixed `HydrologyColumn` realization with a `TerrainMaterializer` contract. The vanilla implementation reports 1.0-block vertical resolution and explicitly exposes partial-block/waterlogging capabilities for future Conquest Reforged adapters.
- `VanillaTerrainMaterializer` lowers only shallow lake/pond beds when necessary to guarantee at least one full Minecraft water block between the continuous Engine bed and water surface.
- Density shaping, synchronous column sampling, surface correction and post-carver hydrology repair all share the same materializer, eliminating mismatched integer quantization between generation phases.
- Dry `LAKE_SHORE` surfaces use climate-sensitive sand or grass/dirt instead of broad gravel fields.
- Extends layout verification for the materializer contract, guaranteed shallow-lake water and additive lake-shore semantics.

## 0.1.0-SNAPSHOT-r24

- Narrows native desert routing to the hottest and driest climate envelope (`temperature > 0.80`, `moisture < 0.28`) so marginal hot/dry regions fall back to plains instead of becoming large deserts.
- Adds `HydrologyCarverGuard` to the Minecraft 1.20.1 reference binding. Vanilla carvers still run, but Engine-owned river/lake/pond beds and water columns are restored immediately afterwards.
- Adds a one-block subsurface bank shell beside materialized Engine water to prevent caves from puncturing a river or lake laterally.
- Wet hydrology bottoms are restored as gravel while the deeper seal uses the generator default block.
- Intended companion engine is r21 or newer, whose bank-contained local water profile prevents mountain-slope overflow before Minecraft materializes the water.

# Changelog

## 0.1.0-SNAPSHOT-r23

- Pair the 1.20.1 host with Engine r18 climate-weighted hydrology: fewer small rivers overall and strongly reduced local river formation in hot/dry catchments while still allowing major rivers to cross deserts.
- Add a shared dry-climate riparian-zone predicate based on river width, accumulated flow and centerline distance.
- Route dry riverbanks to the native plains biome instead of desert so vanilla vegetation generation can form a green fringe around persistent watercourses.
- Force grass-over-dirt on those dry riparian banks after vanilla surface rules, preventing desert sand from running directly to every river edge; the wet channel/lake bed keeps the existing gravel rule.
- Ignore `gradlew`, `gradlew.bat` and `gradle/wrapper/` while retaining the tracked Gradle project configuration under `gradle/`.

## 0.1.0-SNAPSHOT-r22

- Integrate Engine r16 depression-aware hydrology: terrain-guided curved streams/rivers, guaranteed wet-channel depth and pond/lake water surfaces.
- Treat Engine `LAKE` semantics as native river-style aquatic biome routing in the Minecraft 1.20.1 reference adapter.
- Stop forcing every river surface to sand; wet river/lake beds use gravel while sand remains a coast/ocean surface material, removing the circular sand patches seen around channel samples.
- Keep `HydrologyColumn` as the single water materialization path, so river and lake water use Engine-owned continuous surfaces rather than per-column guesses.
- Extend layout verification for lake routing and the no-river-sand surface contract.

## 0.1.0-SNAPSHOT-r21

- Show the external Engine provider version in the F3 debug HUD so snapshot integration can be verified in-game.
- Extend the Engine API `RiverSample` additively with `waterSurfaceHeight` and `flow` while retaining the legacy three-argument constructor for binary/source compatibility.
- Add `RIVER_WATER_LEVEL` as an optional Engine capability.
- Add `HydrologyColumn` to materialize Engine-provided river-water surfaces consistently in both chunk shaping and synchronous column sampling.
- Keep river water tied to the directed drainage segment rather than deriving a separate level from each terrain column.
- Make `getHeight(...)` water-aware: ocean-floor heightmaps return the solid bed while world-surface-style height queries include river/ocean water.
- Extend the F3 debug text with river depth, width, water level, flow and whether the continuous water surface materializes as at least one full Minecraft water block.
- Add CI/layout guards preventing a return to ad-hoc per-column river-height guesses.

## 0.1.0-SNAPSHOT-r20

- Removed per-column vertical translation from the Minecraft 1.20.1 density bridge.
- Vanilla caves, underground strata and aquifers now stay at their absolute world Y coordinates.
- Engine terrain truncates low columns or adds solid substrate for raised terrain instead of shifting the source column.
- Added a six-block pre-carver surface seal to prevent unrelated vanilla caves from becoming broad floating plates or paper-thin roofs at the Engine surface.
- Updated the 1.20.1 integration documentation and test matrix for the absolute-Y substrate model.
- Extended layout verification to reject any reintroduction of delta-based substrate translation.

## 0.1.0-SNAPSHOT-r19

- Fix unsafe Minecraft 1.20.1 fluid remapping that could translate deep vanilla lava/aquifer fluids to the Engine surface and spawn area.
- `EngineDensityBridge` now remaps solid/cave geometry but converts translated underground fluid cells to air; only stable global sea-level water is reconstructed above the Engine surface.
- Remove the provisional per-column highland-river water-level approximation, which could create stepped source-water fronts and chained neighbor-update cascades.
- Avoid writing unchanged block states during the density reshape to reduce chunk-generation work.
- Mark full aquifer-fluid restoration as a deferred, height-stable Minecraft adapter task rather than claiming translated vanilla aquifers are safe.

## 0.1.0-SNAPSHOT-r18

- Declare Fabric API `0.92.2+1.20.1` as an explicit Minecraft 1.20.1 Fabric runtime/build dependency.
- This supplies `fabric-resource-loader-v0`, which exposes mod `data/` resources as a data pack during the dynamic worldgen registry reload.
- Keep the FlTerraForged world preset and `minecraft:normal` tag in `families/mc1201/common/src/main/resources`; no manual data-pack copy is required.
- Extend layout verification so CI rejects a 1.20.1 Fabric adapter that ships a world preset without declaring Fabric API/resource-loader support.

## 0.1.0-SNAPSHOT-r17

- Fix duplicate MC 1.20.1 Fabric resources by keeping world-preset/tag/lang files only in `families/mc1201/common`.
- Fix Gradle 10 deprecation in `processResources` by capturing `version` during configuration instead of accessing `Task.project` at execution time.
- Extend layout verification to reject duplicate relative resource paths and execution-time `project.version` access.

## 0.1.0-SNAPSHOT-r16

- Added the actual `data/flterraforged/worldgen/world_preset/flterraforged.json` datapack resource.
- Added `flterraforged:flterraforged` to the `minecraft:normal` world-preset tag without replacing Vanilla presets.
- Added German and English `generator.flterraforged.flterraforged` translations so the selector shows `FlTerraForged`.
- Extended layout verification to require preset, tag, and translations.

## 0.1.0-SNAPSHOT-r15

- Fix Minecraft 1.20.1 startup crash caused by registering `BIOME_SOURCE` and `CHUNK_GENERATOR` codecs from the late Fabric `main` entrypoint after built-in registries were frozen.
- Register both codecs during vanilla bootstrap via `BiomeSourcesMixin` and `ChunkGeneratorsMixin`.
- Keep `ModInitializer` read-only: it now validates that both bootstrap registrations succeeded.
- Add a layout regression check preventing late worldgen codec registration from returning.


## 0.1.0-SNAPSHOT-r14

- Extended the GitHub Actions verify job to publish the installable Minecraft 1.20.1 Fabric mod JAR after every successful build (push, pull request and manual dispatch).
- Uses `actions/upload-artifact@v7.0.1` on the Node 24 runtime and uploads the JAR directly with `archive: false`.
- Filters sources, Javadoc, dev and shadow artifacts and fails if the build produces zero or multiple installable candidates.
- Renames the downloadable test JAR to include the short commit SHA, retains it for 30 days and writes its authenticated download link plus SHA-256 digest to the GitHub Actions job summary.
- Added layout regression checks for the CI artifact contract.

## 0.1.0-SNAPSHOT-r13

- Added `MC1201-TEST-MATRIX.md`, a gated functional validation matrix for the Minecraft 1.20.1 Fabric reference adapter.
- Covers startup/preset decoding, Engine terrain, climate/biomes, rivers/coasts, NoiseRouter/caves/aquifers, surface rules, vanilla features/ores, structures, chunk boundaries, determinism, performance and multiplayer lifecycle.
- Defines fixed regression seeds, evidence requirements, P0/P1 severity, release criteria and a reusable defect template.

## 0.1.0-SNAPSHOT-r11

- Run the complete Gradle multi-project build on Java 21 because Fabric Loom 1.17.x is itself compiled for Java 21.
- Install Java 17 alongside Java 21 in CI and keep the Minecraft 1.20.1 source/toolchain target at Java 17.
- Apply the same Java 21 Gradle runtime to the Engine API Maven publish job because Gradle configures the Loom-backed `mc1201-fabric` subproject even when only `:engine-api:publish` is requested.
- Add layout verification for the split build-runtime/toolchain contract.

## 0.1.0-SNAPSHOT-r10

- Fixed the Minecraft 1.20.1 seed-access bridge for Fabric Mixins.
- `NoiseConfig` is a concrete/final Minecraft type, so javac rejects a direct `instanceof NoiseConfigSeedAccess` check even though Mixin adds that interface at runtime.
- `EngineWorldSession` now performs the Mixin bridge cast through `Object` and converts a missing Mixin into a descriptive `IllegalStateException`.
- Added a layout regression check for the bridge so this compile-time incompatibility cannot be reintroduced accidentally.

## 0.1.0-SNAPSHOT-r9

- Fixed `mc1201-fabric` dependency resolution when consuming the external Engine snapshot.
- The external Engine POM declares `flterraforged-engine-api` transitively; the host build now excludes that remote transitive module and uses the local `:engine-api` project instead.
- Keeps Loom jar-in-jar deterministic: Engine is included non-transitively, while `:engine-api` and `:common` are embedded explicitly.
- Added layout regression checks so the 1.20.1 adapter cannot accidentally reintroduce a remote API dependency during the host build.

## 0.1.0-SNAPSHOT-r8

- Fixed the Minecraft 1.20.1 Fabric resource source-set configuration: `srcDirs(...)` appended the conventional `src/main/resources` root a second time, causing `fabric.mod.json` to be copied twice by Gradle 9.7.1.
- `mc1201-fabric` now uses `setSrcDirs(...)` for Java and resource roots, making each source root unique.
- CI now runs Gradle with `--warning-mode all` so any remaining Gradle/Loom deprecation is printed with its actual source instead of only the generic Gradle 10 compatibility summary.


- Added the first real Minecraft binding: Fabric 1.20.1.
- Added `FlTerraForgedChunkGenerator` and `FlTerraForgedBiomeSource`.
- Added `NoiseConfigMixin` to bind the external engine to the actual world seed.
- Added the data-driven `flterraforged:flterraforged` world preset with vanilla Nether/End.
- Added native biome routing from engine terrain/climate signals.
- Added FEF-style external Engine Maven resolution and Loom jar-in-jar packaging.
- Added static verification for the MC 1.20.1 reference binding.
- Documented current limitations (carvers, full surface rules, aquifers and density integration are follow-ups).


## 0.1.0-SNAPSHOT-r6

- Added explicit `@param` documentation to all public record compact constructors.
- Fixes Java 17 Javadoc `-Werror` failures (`18 warnings`) during `engine-api:javadoc` / Maven publishing.


## 0.1.0-SNAPSHOT

- Fix: declare the JUnit Platform launcher on the test runtime classpath for Gradle 9.x.

- Initialized clean FlTerraForged architecture.
- Added pure Java 17 `flterraforged-engine-api`.
- Added engine provider, lifecycle, capability and terrain sample contracts.
- Preserved fractional surface height as a first-class API value.
- Added ServiceLoader-ready engine registry in `common`.
- Added provisional Minecraft family matrix from 1.20.1 through 26.2 plus current snapshot.
- Enforced a maximum of one snapshot target.
- Added Fabric/NeoForge, family, exact-version and optional compatibility skeletons.
- Added isolation and layout verification.
- No upstream source code imported yet.

## 0.1.0-SNAPSHOT-r2

- Publishes `flterraforged-engine-api` to GitHub Packages after a successful `main` build.
- Uses `GITHUB_TOKEN` with repository-scoped `packages: write` permission in CI.
- Aligns CI on Gradle 9.7.1.


### r3
- Replaced GitHub Packages publishing with the FEF-style public Maven repository model.
- `engine-api` now publishes into `build/maven-repository`.
- `develop` mirrors the generated Maven repository to the public `maven` branch.
- Added `develop`/`main` workflow separation; snapshots publish only from `develop`.
- Consumers no longer require GitHub Packages credentials.


### r4
- Updated GitHub Actions to Node 24 compatible releases: `actions/checkout@v6`, `actions/setup-java@v6`, and `gradle/actions/setup-gradle@v6`.
- Pinned `peaceiris/actions-gh-pages@v4.1.0`, whose action runtime uses Node 24.
- Removes Node 20 deprecation warnings from the build and Maven-branch publishing workflow.

## 0.1.0-SNAPSHOT-r5

- Added complete Javadoc coverage for all public Engine API members.
- Added package-level API documentation.
- Configured Javadoc with `-Werror` so Maven publication cannot silently emit Javadoc warnings.

## r12 - functional Minecraft 1.20.1 worldgen binding

- Replaced the column-only 1.20.1 noise fill with a hybrid vanilla/Engine pipeline.
- Added `VanillaWorldgenDelegate` around Minecraft 1.20.1 `NoiseChunkGenerator`.
- Added `EngineDensityBridge` to remap vanilla NoiseRouter/aquifer columns to Engine heights.
- Delegated vanilla `buildSurface`, AIR/LIQUID `carve`, and `populateEntities`.
- Kept vanilla feature/ore generation on the inherited `ChunkGenerator.generateFeatures` path.
- Added `EngineSurfaceGuard` for Engine-specific coast/river/snow semantics and surface fallback.
- Added `MC1201-FUNCTIONAL-WORLDGEN.md` documenting stage ownership and known boundaries.
