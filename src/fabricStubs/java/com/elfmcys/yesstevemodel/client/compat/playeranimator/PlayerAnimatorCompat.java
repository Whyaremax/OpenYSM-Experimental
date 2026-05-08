package com.elfmcys.yesstevemodel.client.compat.playeranimator;

import net.minecraft.client.player.AbstractClientPlayer;

public final class PlayerAnimatorCompat {
    private PlayerAnimatorCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isPlayerAnimated(AbstractClientPlayer player) {
        return false;
    }
}
