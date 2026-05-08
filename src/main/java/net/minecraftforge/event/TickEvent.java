package net.minecraftforge.event;

import net.minecraft.server.MinecraftServer;

public class TickEvent {
    public enum Phase {
        START,
        END
    }

    public static class ServerTickEvent extends TickEvent {
        public final Phase phase;
        private final MinecraftServer server;

        public ServerTickEvent(MinecraftServer server, Phase phase) {
            this.server = server;
            this.phase = phase;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static class ClientTickEvent extends TickEvent {
        public final Phase phase;

        public ClientTickEvent(Phase phase) {
            this.phase = phase;
        }
    }
}
