package net.minecraftforge.fml;

import java.util.stream.Stream;

public final class InterModComms {
    private InterModComms() {
    }

    public static Stream<IMCMessage> getMessages(String modId) {
        return Stream.empty();
    }

    public record IMCMessage(String method, java.util.function.Supplier<?> messageSupplier) {
    }
}
