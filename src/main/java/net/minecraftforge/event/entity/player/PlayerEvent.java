package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerEvent {
    private final Player entity;

    public PlayerEvent(Player entity) {
        this.entity = entity;
    }

    public Player getEntity() { return entity; }

    public static class Clone extends PlayerEvent {
        private final Player original;

        public Clone(Player original, Player entity) {
            super(entity);
            this.original = original;
        }

        public Player getOriginal() { return original; }
    }

    public static class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(Player entity, Entity target) {
            super(entity);
            this.target = target;
        }

        public Entity getTarget() { return target; }
    }

    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player entity) {
            super(entity);
        }
    }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player entity) {
            super(entity);
        }
    }
}
