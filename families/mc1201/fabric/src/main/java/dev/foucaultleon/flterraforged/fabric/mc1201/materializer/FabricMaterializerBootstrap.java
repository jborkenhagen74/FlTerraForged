package dev.foucaultleon.flterraforged.fabric.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializerProvider;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerRegistry;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.MaterializerRuntime;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard.VanillaBlockMaterializerProvider;
import net.fabricmc.loader.api.FabricLoader;

/** Discovers Fabric materializer entrypoints and installs the configured provider. */
public final class FabricMaterializerBootstrap {

    /** Fabric entrypoint key implemented by external materializer provider mods. */
    public static final String ENTRYPOINT_KEY = "flterraforged:materializer";

    private FabricMaterializerBootstrap() {
    }

    /** Discovers providers, reads configuration and installs the selected provider. */
    public static void bootstrap() {
        MaterializerRegistry registry = new MaterializerRegistry();
        registry.register(new VanillaBlockMaterializerProvider());
        FabricLoader loader = FabricLoader.getInstance();
        for (BlockMaterializerProvider provider
                : loader.getEntrypoints(ENTRYPOINT_KEY, BlockMaterializerProvider.class)) {
            registry.register(provider);
        }
        registry.freeze();

        MaterializerConfig config = MaterializerConfig.load(loader.getConfigDir());
        try {
            registry.require(config.materializerId());
        } catch (IllegalStateException exception) {
            throw new IllegalStateException(
                    "Invalid FlTerraForged materializer selection in " + config.path()
                            + ". " + exception.getMessage(),
                    exception);
        }
        MaterializerRuntime.install(registry, config.materializerId(), config.options());
    }
}
