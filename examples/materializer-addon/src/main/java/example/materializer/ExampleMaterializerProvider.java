package example.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializerProvider;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard.VanillaBlockMaterializer;

/** Example external provider discovered by FlTerraForged through a Fabric entrypoint. */
public final class ExampleMaterializerProvider implements BlockMaterializerProvider {

    @Override
    public String id() {
        return "example:custom";
    }

    @Override
    public String displayName() {
        return "Example custom materializer";
    }

    @Override
    public BlockMaterializer create(MaterializerContext context) {
        return new ExampleMaterializer(new VanillaBlockMaterializer(context));
    }
}
