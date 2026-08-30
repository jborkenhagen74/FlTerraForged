package dev.foucaultleon.flterraforged.fabric.mc1201;

import net.fabricmc.api.ModInitializer;

/** Fabric bootstrap for the Minecraft 1.20.1 reference binding. */
public final class FlTerraForgedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FlTerraForgedWorldgenRegistries.register();
    }
}
