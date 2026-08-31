# External materializer add-on example

This directory is intentionally **not** part of the FlTerraForged root build. It is a reference for a
separate Fabric mod that provides a replacement block materializer.

1. Compile the add-on against Minecraft 1.20.1, Fabric Loader and the matching FlTerraForged JAR.
2. Implement `BlockMaterializerProvider`.
3. Register the provider through the `flterraforged:materializer` Fabric entrypoint.
4. Install both mods.
5. Start once so FlTerraForged creates `config/flterraforged/materializer.properties`.
6. Set:

```properties
materializer=example:custom
```

7. Restart before generating new chunks.

The example extends `DelegatingBlockMaterializer` and wraps FlTerraForged's standard
`VanillaBlockMaterializer`; only the river/lake bed block is changed. A completely independent
implementation can implement `BlockMaterializer` directly.

FlTerraForged fails startup when the configured provider is not installed. It deliberately does not
silently fall back to vanilla, because doing so could create permanent world-generation seams.
