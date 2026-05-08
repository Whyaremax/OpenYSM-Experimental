package net.minecraftforge.eventbus.api;

public interface IEventBus {
    default boolean post(Event event) {
        return event != null && event.isCanceled();
    }

    default <T> void addListener(T listener) {
    }
}
