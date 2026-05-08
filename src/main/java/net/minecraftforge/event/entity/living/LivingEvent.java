package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;

public class LivingEvent {
    private final LivingEntity entity;

    public LivingEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public static class LivingTickEvent extends LivingEvent {
        public LivingTickEvent(LivingEntity entity) {
            super(entity);
        }
    }
}
