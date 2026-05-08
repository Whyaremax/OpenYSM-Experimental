package net.minecraftforge.fml;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public final class DistExecutor {
    private DistExecutor() {
    }

    public static void safeRunWhenOn(Dist dist, Supplier<Runnable> runnable) {
        if (FMLEnvironment.dist == dist) {
            runnable.get().run();
        }
    }
}
