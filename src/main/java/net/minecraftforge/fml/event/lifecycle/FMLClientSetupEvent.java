package net.minecraftforge.fml.event.lifecycle;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FMLClientSetupEvent {
    private static final Queue<Runnable> QUEUED_WORK = new ConcurrentLinkedQueue<>();

    public void enqueueWork(Runnable runnable) {
        QUEUED_WORK.add(runnable);
    }

    public static void runQueuedWork() {
        Runnable runnable;
        while ((runnable = QUEUED_WORK.poll()) != null) {
            runnable.run();
        }
    }
}
