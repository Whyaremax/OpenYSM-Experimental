package net.minecraftforge.fml;

public final class ModLoader {
    private static final ModLoader INSTANCE = new ModLoader();

    private ModLoader() {
    }

    public static ModLoader get() {
        return INSTANCE;
    }

    public void addWarning(ModLoadingWarning warning) {
    }
}
