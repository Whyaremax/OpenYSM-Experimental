package com.elfmcys.yesstevemodel.client.compat.cosmeticarmorreworked;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticArmorHelper {
    private CosmeticArmorHelper() {
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    public static ItemStack getElytraItem(LivingEntity entity) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() == Items.ELYTRA ? stack : ItemStack.EMPTY;
    }
}
