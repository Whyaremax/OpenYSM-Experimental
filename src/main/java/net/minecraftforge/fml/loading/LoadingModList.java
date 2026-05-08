package net.minecraftforge.fml.loading;

import net.minecraftforge.fml.ModList;

public final class LoadingModList {
    private LoadingModList() {
    }

    public static ModList get() {
        return ModList.get();
    }
}
