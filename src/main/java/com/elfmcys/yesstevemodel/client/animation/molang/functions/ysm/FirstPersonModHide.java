package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.client.compatibility.YsmClientCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.Player;

public class FirstPersonModHide implements IValueEvaluator<Boolean, IContext<Player>> {
    @Override
    public Boolean eval(IContext<Player> ctx) {
        if (!ctx.animationEvent().isFirstPerson() && YsmClientCompat.isFirstPersonCompatLoaded() && CameraUtil.getCameraType(ctx) == CameraType.FIRST_PERSON.ordinal()) {
            return YsmClientCompat.shouldHideHead();
        }
        return false;
    }
}
