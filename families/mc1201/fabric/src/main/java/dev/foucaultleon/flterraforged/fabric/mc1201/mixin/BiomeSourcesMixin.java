package dev.foucaultleon.flterraforged.fabric.mc1201.mixin;

import com.mojang.serialization.Codec;
import dev.foucaultleon.flterraforged.fabric.mc1201.FlTerraForgedWorldgenRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Injects the custom biome-source codec while the built-in registry is still mutable. */
@Mixin(BiomeSources.class)
public abstract class BiomeSourcesMixin {

    @Inject(method = "registerAndGetDefault", at = @At("HEAD"))
    private static void flterraforged$registerBiomeSource(
            Registry<Codec<? extends BiomeSource>> registry,
            CallbackInfoReturnable<Codec<? extends BiomeSource>> callback) {
        FlTerraForgedWorldgenRegistries.registerBiomeSource(registry);
    }
}
