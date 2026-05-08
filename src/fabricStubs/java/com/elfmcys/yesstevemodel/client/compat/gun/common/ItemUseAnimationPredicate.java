package com.elfmcys.yesstevemodel.client.compat.gun.common;

import com.elfmcys.yesstevemodel.client.animation.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public final class ItemUseAnimationPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        return PlayState.STOP;
    }

    public static boolean isLoaded() {
        return false;
    }
}
