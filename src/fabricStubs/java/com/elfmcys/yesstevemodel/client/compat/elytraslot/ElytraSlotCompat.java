package com.elfmcys.yesstevemodel.client.compat.elytraslot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ElytraSlotCompat {
    private ElytraSlotCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static ItemStack getElytraItem(LivingEntity entity) {
        return ItemStack.EMPTY;
    }
}
