package com.elfmcys.yesstevemodel.client.compatibility;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class YsmClientCompat {
    private YsmClientCompat() {
    }

    public static boolean isPbrActive() {
        return false;
    }

    public static void updatePbrState() {
    }

    public static boolean isOculusLoaded() {
        return false;
    }

    public static boolean isOptifinePresent() {
        return false;
    }

    public static boolean isFirstPersonCompatLoaded() {
        return false;
    }

    public static boolean isFirstPersonActive() {
        return false;
    }

    public static boolean shouldHideHead() {
        return false;
    }

    public static void setCameraDistance(float distance) {
    }

    public static boolean isRealCameraActive() {
        return false;
    }

    public static boolean isPlayerAnimatorAnimated(AbstractClientPlayer player) {
        return false;
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    public static ItemStack getElytraItem(LivingEntity entity) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() == Items.ELYTRA ? stack : ItemStack.EMPTY;
    }
}
