package net.minecraftforge.common.capabilities;

import java.util.concurrent.atomic.AtomicInteger;

public final class CapabilityManager {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private CapabilityManager() {
    }

    public static <T> Capability<T> get(CapabilityToken<T> token) {
        return new Capability<>(NEXT_ID.incrementAndGet());
    }
}
