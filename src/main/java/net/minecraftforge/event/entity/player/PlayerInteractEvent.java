package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;

public class PlayerInteractEvent extends PlayerEvent {
    public PlayerInteractEvent(Player entity) {
        super(entity);
    }

    public static class EntityInteract extends PlayerInteractEvent {
        private final net.minecraft.world.entity.Entity target;

        public EntityInteract(Player player, net.minecraft.world.entity.Entity target) {
            super(player);
            this.target = target;
        }

        public net.minecraft.world.entity.Entity getTarget() { return target; }
    }
}
