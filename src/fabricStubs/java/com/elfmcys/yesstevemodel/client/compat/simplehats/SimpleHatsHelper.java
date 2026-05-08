package com.elfmcys.yesstevemodel.client.compat.simplehats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SimpleHatsHelper {
    private SimpleHatsHelper() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    @Nullable
    public static ItemStack getHatItem(LivingEntity entity) {
        return null;
    }
}
