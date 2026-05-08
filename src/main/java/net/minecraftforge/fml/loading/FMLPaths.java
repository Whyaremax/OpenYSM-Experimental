package net.minecraftforge.fml.loading;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.function.Supplier;

public enum FMLPaths implements Supplier<Path> {
    GAMEDIR {
        @Override
        public Path get() {
            return FabricLoader.getInstance().getGameDir();
        }
    },
    CONFIGDIR {
        @Override
        public Path get() {
            return FabricLoader.getInstance().getConfigDir();
        }
    }
}
