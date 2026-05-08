package com.elfmcys.yesstevemodel.client.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;

public final class ParcoolCompat {
    private ParcoolCompat() {
    }

    public static void init() {
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return Optional.empty();
    }

    public static boolean isLoaded() {
        return false;
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return Optional.empty();
    }

    public static boolean isPlayerParcooling(Player player) {
        return false;
    }

    @Nullable
    public static String getActionName(Player player) {
        return null;
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.livingEntityVar("parcool_state", ctx -> StringPool.EMPTY);
    }
}
