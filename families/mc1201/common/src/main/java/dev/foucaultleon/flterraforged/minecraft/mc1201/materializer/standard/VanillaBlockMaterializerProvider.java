package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializerProvider;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;

/** Built-in provider for FlTerraForged's vanilla-compatible materializer. */
public final class VanillaBlockMaterializerProvider implements BlockMaterializerProvider {

    /** Stable provider identifier used by the default configuration. */
    public static final String ID = "flterraforged:vanilla";

    /** Creates the built-in provider. */
    public VanillaBlockMaterializerProvider() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "FlTerraForged Vanilla";
    }

    @Override
    public BlockMaterializer create(MaterializerContext context) {
        return new VanillaBlockMaterializer(context);
    }
}
