package dev.foucaultleon.flterraforged.fabric.mc1201.mixin;

import dev.foucaultleon.flterraforged.minecraft.mc1201.access.NoiseConfigSeedAccess;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the world seed that Minecraft 1.20.1 keeps private in {@link NoiseConfig}. */
@Mixin(NoiseConfig.class)
public abstract class NoiseConfigMixin implements NoiseConfigSeedAccess {

    @Unique
    private long flterraforged$seed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void flterraforged$captureSeed(
            ChunkGeneratorSettings settings,
            RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup,
            long seed,
            CallbackInfo callbackInfo) {
        this.flterraforged$seed = seed;
    }

    @Override
    public long flterraforged$getSeed() {
        return flterraforged$seed;
    }
}
