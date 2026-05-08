package net.minecraftforge.client.event;

import net.minecraft.client.player.LocalPlayer;

public class ClientPlayerNetworkEvent {
    public static class Clone extends ClientPlayerNetworkEvent {
        private final LocalPlayer oldPlayer;
        private final LocalPlayer newPlayer;

        public Clone(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
            this.oldPlayer = oldPlayer;
            this.newPlayer = newPlayer;
        }

        public LocalPlayer getOldPlayer() {
            return oldPlayer;
        }

        public LocalPlayer getNewPlayer() {
            return newPlayer;
        }
    }

    public static class LoggingIn extends ClientPlayerNetworkEvent {
    }

    public static class LoggingOut extends ClientPlayerNetworkEvent {
    }
}
