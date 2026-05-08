package com.elfmcys.yesstevemodel.client.compat.ironsspellbooks;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class SpellbooksCompat {
    private SpellbooksCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.clientPlayerEntityVar("iss_animation", ctx -> StringPool.EMPTY);
    }

    @Nullable
    public static PlayState resolvePlayState(AnimationEvent<LivingAnimatable<?>> event, LivingEntity entity) {
        return null;
    }
}
