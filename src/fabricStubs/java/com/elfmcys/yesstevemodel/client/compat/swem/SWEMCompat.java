package com.elfmcys.yesstevemodel.client.compat.swem;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class SWEMCompat {
    private SWEMCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    @Nullable
    public static String getHorseGaitName(LivingEntity entity) {
        return null;
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        binding.livingEntityVar("swem_is_ride", ctx -> false);
        binding.livingEntityVar("swem_state", ctx -> StringPool.EMPTY);
    }
}
