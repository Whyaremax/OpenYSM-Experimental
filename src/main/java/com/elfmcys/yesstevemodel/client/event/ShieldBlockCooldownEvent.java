package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public class ShieldBlockCooldownEvent {

    public static final String TAG_KEY = "ysm$shield_block_cooldown";
    private static final Map<LivingEntity, Integer> COOLDOWNS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        COOLDOWNS.put(event.getEntity(), 5);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Integer cooldown = COOLDOWNS.get(entity);
        if (cooldown != null) {
            int i = cooldown.intValue();
            if (i > 0) {
                COOLDOWNS.put(entity, i - 1);
            } else {
                COOLDOWNS.remove(entity);
            }
        }
    }

    public static boolean isOnCooldown(LivingEntity livingEntity) {
        return COOLDOWNS.containsKey(livingEntity);
    }
}
