package net.minecraftforge.event;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AttachCapabilitiesEvent<T> {
    private final T object;
    private final Map<ResourceLocation, Object> capabilities = new HashMap<>();

    public AttachCapabilitiesEvent(T object) {
        this.object = object;
    }

    public T getObject() { return object; }
    public Map<ResourceLocation, Object> getCapabilities() { return capabilities; }
    public void addCapability(ResourceLocation id, Object provider) { capabilities.put(id, provider); }
}
