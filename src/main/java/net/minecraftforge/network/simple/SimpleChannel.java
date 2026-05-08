package net.minecraftforge.network.simple;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleChannel {
    private final ResourceLocation id;
    private final Map<Integer, Registration<?>> serverbound = new HashMap<>();
    private final Map<Integer, Registration<?>> clientbound = new HashMap<>();
    private final Map<Class<?>, Registration<?>> byType = new HashMap<>();
    private boolean registered;

    public SimpleChannel(ResourceLocation id) {
        this.id = id;
    }

    public <MSG> void registerMessage(int packetId, Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler, Optional<NetworkDirection> direction) {
        NetworkDirection resolved = direction.orElse(NetworkDirection.PLAY_TO_CLIENT);
        Registration<MSG> registration = new Registration<>(packetId, type, encoder, decoder, handler, resolved);
        byType.put(type, registration);
        if (resolved == NetworkDirection.PLAY_TO_SERVER) {
            serverbound.put(packetId, registration);
        } else {
            clientbound.put(packetId, registration);
        }
    }

    public void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            FriendlyByteBuf copy = copy(buf);
            server.execute(() -> receiveServerbound(copy, NetworkHandler.getConnection(player), player));
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkingBridge.register(this);
        }
    }

    public void receiveClientbound(FriendlyByteBuf buf, Connection connection) {
        receive(buf, connection, null, NetworkDirection.PLAY_TO_CLIENT);
    }

    private void receiveServerbound(FriendlyByteBuf buf, Connection connection, ServerPlayer sender) {
        receive(buf, connection, sender, NetworkDirection.PLAY_TO_SERVER);
    }

    private void receive(FriendlyByteBuf buf, Connection connection, ServerPlayer sender, NetworkDirection direction) {
        int packetId = buf.readVarInt();
        Registration<?> registration = (direction == NetworkDirection.PLAY_TO_SERVER ? serverbound : clientbound).get(packetId);
        if (registration == null) {
            return;
        }
        registration.handle(buf, new NetworkEvent.Context(direction, connection, sender));
    }

    public void sendToServer(Object message) {
        ensureRegistered();
        FriendlyByteBuf buf = encode(message);
        if (buf != null) {
            ClientNetworkingBridge.send(id, buf);
        }
    }

    public void send(PacketDistributor.PacketTarget target, Object message) {
        ensureRegistered();
        target.send(this, message);
    }

    public void sendToPlayer(ServerPlayer player, Object message) {
        if (player == null) {
            return;
        }
        FriendlyByteBuf buf = encode(message);
        if (buf != null) {
            ServerPlayNetworking.send(player, id, buf);
        }
    }

    public void sendToAllPlayers(Object message) {
        FriendlyByteBuf buf = encode(message);
        if (buf == null) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, id, copy(buf));
        }
    }

    public void sendToTracking(Entity entity, Object message, boolean includeSelf) {
        FriendlyByteBuf buf = encode(message);
        if (buf == null || entity == null) {
            return;
        }
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, id, copy(buf));
        }
        if (includeSelf && entity instanceof ServerPlayer player) {
            ServerPlayNetworking.send(player, id, copy(buf));
        }
    }

    public void reply(Object message, NetworkEvent.Context context) {
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            sendToServer(message);
        } else if (context.getSender() != null) {
            sendToPlayer(context.getSender(), message);
        }
    }

    public Packet<?> toVanillaPacket(Object message, NetworkDirection direction) {
        FriendlyByteBuf buf = encode(message);
        if (buf == null) {
            return null;
        }
        return direction == NetworkDirection.PLAY_TO_SERVER
                ? new ServerboundCustomPayloadPacket(id, buf)
                : new ClientboundCustomPayloadPacket(id, buf);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FriendlyByteBuf encode(Object message) {
        Registration registration = byType.get(message.getClass());
        if (registration == null) {
            return null;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(registration.packetId);
        registration.encoder.accept(message, buf);
        return buf;
    }

    private static FriendlyByteBuf copy(FriendlyByteBuf source) {
        FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(source.readableBytes()));
        copy.writeBytes(source, source.readerIndex(), source.readableBytes());
        return copy;
    }

    private record Registration<MSG>(int packetId, Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction) {
        void handle(FriendlyByteBuf buf, NetworkEvent.Context context) {
            MSG message = decoder.apply(buf);
            handler.accept(message, () -> context);
        }
    }
}
