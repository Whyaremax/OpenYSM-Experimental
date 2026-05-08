package net.minecraftforge.fml;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public final class ModLoadingContext {
    private static final ModLoadingContext INSTANCE = new ModLoadingContext();

    private ModLoadingContext() {
    }

    public static ModLoadingContext get() {
        return INSTANCE;
    }

    public void registerConfig(ModConfig.Type type, ForgeConfigSpec spec) {
    }
}
