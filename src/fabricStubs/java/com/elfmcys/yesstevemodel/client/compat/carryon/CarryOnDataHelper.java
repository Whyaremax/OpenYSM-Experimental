package com.elfmcys.yesstevemodel.client.compat.carryon;

import net.minecraft.world.entity.player.Player;

public final class CarryOnDataHelper {
    private CarryOnDataHelper() {
    }

    public enum CarryType {
        ENTITY,
        BLOCK,
        PLAYER,
        NONE
    }

    public static CarryType getCarryType(Player player) {
        return CarryType.NONE;
    }
}
