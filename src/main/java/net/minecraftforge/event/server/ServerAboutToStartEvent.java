package net.minecraftforge.event.server;

import net.minecraft.server.MinecraftServer;

public class ServerAboutToStartEvent {
    private final MinecraftServer server;

    public ServerAboutToStartEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
