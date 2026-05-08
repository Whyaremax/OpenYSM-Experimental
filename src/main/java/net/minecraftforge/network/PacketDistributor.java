package net.minecraftforge.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class PacketDistributor {
    public static final PlayerDistributor PLAYER = new PlayerDistributor();
    public static final AllDistributor ALL = new AllDistributor();
    public static final TrackingEntityDistributor TRACKING_ENTITY = new TrackingEntityDistributor(false);
    public static final TrackingEntityDistributor TRACKING_ENTITY_AND_SELF = new TrackingEntityDistributor(true);

    private PacketDistributor() {
    }

    public interface PacketTarget {
        void send(SimpleChannel channel, Object message);
    }

    public static final class PlayerDistributor {
        public PacketTarget with(Supplier<ServerPlayer> supplier) {
            return (channel, message) -> channel.sendToPlayer(supplier.get(), message);
        }
    }

    public static final class AllDistributor {
        public PacketTarget noArg() {
            return SimpleChannel::sendToAllPlayers;
        }
    }

    public static final class TrackingEntityDistributor {
        private final boolean includeSelf;

        private TrackingEntityDistributor(boolean includeSelf) {
            this.includeSelf = includeSelf;
        }

        public PacketTarget with(Supplier<? extends Entity> supplier) {
            return (channel, message) -> channel.sendToTracking(supplier.get(), message, includeSelf);
        }
    }
}
