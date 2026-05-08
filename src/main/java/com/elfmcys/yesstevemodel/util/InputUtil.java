package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;

public class InputUtil {
    @SuppressWarnings({"deprecation", "removal"})
    public static boolean isKeyPressed(InputEvent.Key key, KeyMapping keyMapping) {
        return keyMapping.matches(key.getKey(), key.getScanCode());
    }

    public static boolean isPlayerReady() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null || minecraft.screen != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return minecraft.isWindowActive();
    }
}
