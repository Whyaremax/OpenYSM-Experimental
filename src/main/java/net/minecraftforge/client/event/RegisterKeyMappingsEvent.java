package net.minecraftforge.client.event;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class RegisterKeyMappingsEvent {
    public void register(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }
}
