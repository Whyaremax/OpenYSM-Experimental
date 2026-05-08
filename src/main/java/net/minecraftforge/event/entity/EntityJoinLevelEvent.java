package net.minecraftforge.event.entity;

import net.minecraft.world.entity.Entity;

public class EntityJoinLevelEvent {
    private final Entity entity;

    public EntityJoinLevelEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
