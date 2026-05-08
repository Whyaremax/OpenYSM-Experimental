package com.elfmcys.yesstevemodel.client.compat.sbackpack;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SBackpackCompat {
    private SBackpackCompat() {
    }

    public static void init() {
    }

    public static void setupRenderLayers() {
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return Optional.empty();
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        binding.livingEntityVar("has_sophisticated_backpack", ctx -> false);
    }

    @Nullable
    public static ItemStack getBackpackItem(LivingEntity entity) {
        return null;
    }
}
