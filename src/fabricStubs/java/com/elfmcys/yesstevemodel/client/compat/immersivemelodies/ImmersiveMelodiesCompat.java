package com.elfmcys.yesstevemodel.client.compat.immersivemelodies;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;

public final class ImmersiveMelodiesCompat {
    private ImmersiveMelodiesCompat() {
    }

    public static void init() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void updateMelodyProgress(LivingEntity entity, ImmersiveMelodiesData data) {
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.livingEntityVar("im_pitch", ctx -> 0.0f);
        binding.livingEntityVar("im_volume", ctx -> 0.0f);
        binding.livingEntityVar("im_current", ctx -> 0.0f);
        binding.livingEntityVar("im_delta", ctx -> 0L);
        binding.livingEntityVar("im_time", ctx -> 0L);
    }

    public static final class ImmersiveMelodiesData {
        public float pitch;
        public float volume;
        public float current;
        public long delta;
        public long time;
    }
}
