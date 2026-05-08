package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;

public class ShieldBlockEvent extends LivingEvent {
    public ShieldBlockEvent(LivingEntity entity) {
        super(entity);
    }
}
