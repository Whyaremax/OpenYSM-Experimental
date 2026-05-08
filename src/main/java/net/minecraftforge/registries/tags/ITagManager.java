package net.minecraftforge.registries.tags;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.stream.StreamSupport;

public class ITagManager<T> {
    private final Registry<T> registry;
    private final ResourceKey<? extends Registry<T>> registryKey;

    public ITagManager(Registry<T> registry) {
        this.registry = registry;
        this.registryKey = registry.key();
    }

    public ITagManager(ResourceKey<? extends Registry<T>> registryKey) {
        this.registry = null;
        this.registryKey = registryKey;
    }

    public TagKey<T> createTagKey(ResourceLocation id) {
        return TagKey.create(registryKey, id);
    }

    public Iterable<T> getTag(TagKey<T> tag) {
        if (registry == null) {
            return List.of();
        }
        return StreamSupport.stream(registry.getTagOrEmpty(tag).spliterator(), false).map(Holder::value).toList();
    }
}
