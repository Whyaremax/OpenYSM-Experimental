package net.minecraftforge.fml;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraftforge.forgespi.locating.IModFile;

import java.util.List;
import java.util.Optional;

public final class ModList {
    private static final ModList INSTANCE = new ModList();

    private ModList() {
    }

    public static ModList get() {
        return INSTANCE;
    }

    public boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public Optional<ModContainer> getModContainerById(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(ModContainer::new);
    }

    public ModFileInfo getModFileById(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(ModFileInfo::new).orElse(null);
    }

    public List<ModInfo> getMods() {
        return FabricLoader.getInstance().getAllMods().stream().map(ModInfo::new).toList();
    }

    public static final class ModContainer {
        private final net.fabricmc.loader.api.ModContainer container;

        public ModContainer(net.fabricmc.loader.api.ModContainer container) {
            this.container = container;
        }

        public ModInfo getModInfo() {
            return new ModInfo(container);
        }
    }

    public static class ModFileInfo implements IModFile {
        private final net.fabricmc.loader.api.ModContainer container;

        public ModFileInfo(net.fabricmc.loader.api.ModContainer container) {
            this.container = container;
        }

        public String versionString() {
            return container.getMetadata().getVersion().getFriendlyString();
        }

        public ModFileInfo getFile() {
            return this;
        }

        @Override
        public SecureJar getSecureJar() {
            return new SecureJar(container.getRootPath());
        }

        public List<ModInfo> getModInfos() {
            return List.of(new ModInfo(container));
        }

        public List<ModInfo> getMods() {
            return getModInfos();
        }
    }

    public static class ModInfo {
        private final ModMetadata metadata;

        public ModInfo(net.fabricmc.loader.api.ModContainer container) {
            this.metadata = container.getMetadata();
        }

        public String getModId() {
            return metadata.getId();
        }

        public String getDisplayName() {
            return metadata.getName();
        }

        public Object getVersion() {
            return metadata.getVersion().getFriendlyString();
        }
    }
}
