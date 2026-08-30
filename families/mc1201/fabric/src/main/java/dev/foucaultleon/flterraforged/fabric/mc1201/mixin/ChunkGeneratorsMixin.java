package dev.foucaultleon.flterraforged.fabric.mc1201.mixin;

import com.mojang.serialization.Codec;
import dev.foucaultleon.flterraforged.fabric.mc1201.FlTerraForgedWorldgenRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGenerators;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Injects the custom chunk-generator codec while the built-in registry is still mutable. */
@Mixin(ChunkGenerators.class)
public abstract class ChunkGeneratorsMixin {

    @Inject(method = "registerAndGetDefault", at = @At("HEAD"))
    private static void flterraforged$registerChunkGenerator(
            Registry<Codec<? extends ChunkGenerator>> registry,
            CallbackInfoReturnable<Codec<? extends ChunkGenerator>> callback) {
        FlTerraForgedWorldgenRegistries.registerChunkGenerator(registry);
    }
}
