package net.minecraftforge.network.simple;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

final class ClientNetworkingBridge {
    private ClientNetworkingBridge() {
    }

    static void register(SimpleChannel channel) {
        ClientPlayNetworking.registerGlobalReceiver(channelId(channel), (client, handler, buf, responseSender) -> {
            FriendlyByteBuf copy = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer(buf.readableBytes()));
            copy.writeBytes(buf, buf.readerIndex(), buf.readableBytes());
            client.execute(() -> channel.receiveClientbound(copy, handler.getConnection()));
        });
    }

    static void send(ResourceLocation id, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }

    private static ResourceLocation channelId(SimpleChannel channel) {
        try {
            java.lang.reflect.Field field = SimpleChannel.class.getDeclaredField("id");
            field.setAccessible(true);
            return (ResourceLocation) field.get(channel);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
