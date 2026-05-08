package net.minecraftforge.network;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class NetworkEvent {
    private NetworkEvent() {
    }

    public static class Context {
        private final NetworkDirection direction;
        private final Connection connection;
        @Nullable
        private final ServerPlayer sender;

        public Context(NetworkDirection direction, Connection connection, @Nullable ServerPlayer sender) {
            this.direction = direction;
            this.connection = connection;
            this.sender = sender;
        }

        public NetworkDirection getDirection() {
            return direction;
        }

        public Connection getNetworkManager() {
            return connection;
        }

        @Nullable
        public ServerPlayer getSender() {
            return sender;
        }

        public void enqueueWork(Runnable runnable) {
            runnable.run();
        }

        public void setPacketHandled(boolean handled) {
        }
    }
}
