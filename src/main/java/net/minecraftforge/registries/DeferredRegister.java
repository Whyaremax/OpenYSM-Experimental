package net.minecraftforge.registries;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DeferredRegister<T> {
    private final Registry<T> registry;
    private final String namespace;
    private final List<Pair<String, Supplier<? extends T>>> entries = new ArrayList<>();

    private DeferredRegister(Registry<T> registry, String namespace) {
        this.registry = registry;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ForgeRegistries.RegistryFacade<T> registry, String namespace) {
        return new DeferredRegister<>(registry.vanilla(), namespace);
    }

    public void register(String name, Supplier<? extends T> supplier) {
        entries.add(Pair.of(name, supplier));
    }

    public void register(IEventBus bus) {
        for (Pair<String, Supplier<? extends T>> entry : entries) {
            Registry.register(registry, new ResourceLocation(namespace, entry.getFirst()), entry.getSecond().get());
        }
    }
}
