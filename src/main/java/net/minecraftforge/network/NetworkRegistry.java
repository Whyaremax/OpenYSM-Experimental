package net.minecraftforge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class NetworkRegistry {
    private NetworkRegistry() {
    }

    public static SimpleChannel newSimpleChannel(ResourceLocation id, Supplier<String> versionSupplier, java.util.function.Predicate<String> clientAccepted, java.util.function.Predicate<String> serverAccepted) {
        return new SimpleChannel(id);
    }
}
