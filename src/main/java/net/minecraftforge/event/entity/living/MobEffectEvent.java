package net.minecraftforge.event.entity.living;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectEvent {
    private final LivingEntity entity;

    protected MobEffectEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public static class Added extends MobEffectEvent {
        private final MobEffectInstance effectInstance;

        public Added(LivingEntity entity, MobEffectInstance effectInstance) {
            super(entity);
            this.effectInstance = effectInstance;
        }

        public MobEffectInstance getEffectInstance() {
            return effectInstance;
        }
    }

    public static class Remove extends MobEffectEvent {
        private final MobEffect effect;

        public Remove(LivingEntity entity, MobEffect effect) {
            super(entity);
            this.effect = effect;
        }

        public MobEffect getEffect() {
            return effect;
        }
    }

    public static class Expired extends MobEffectEvent {
        private final MobEffectInstance effectInstance;

        public Expired(LivingEntity entity, MobEffectInstance effectInstance) {
            super(entity);
            this.effectInstance = effectInstance;
        }

        public MobEffectInstance getEffectInstance() {
            return effectInstance;
        }
    }
}
