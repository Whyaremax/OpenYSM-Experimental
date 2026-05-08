package com.elfmcys.yesstevemodel.capability;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.capabilities.Capability;

final class YsmClientCapabilities {
    private YsmClientCapabilities() {
    }

    static Object create(Object target, Capability<?> capability) {
        if (target instanceof AbstractClientPlayer player && capability == PlayerCapabilityProvider.PLAYER_CAP) {
            return new PlayerCapability(player);
        }
        if (target instanceof Projectile projectile && capability == ProjectileCapabilityProvider.PROJECTILE_CAP) {
            return new ProjectileCapability(projectile);
        }
        if (target instanceof Entity entity && capability == VehicleCapabilityProvider.VEHICLE_CAP) {
            return new VehicleCapability(entity);
        }
        if (target instanceof Entity entity && capability == ClientLazyCapabilityProvider.CLIENT_LAZY_CAP) {
            ProjectileCapabilityProvider projectile = entity instanceof Projectile p ? new ProjectileCapabilityProvider(p) : null;
            return new ClientLazyCapability(new VehicleCapabilityProvider(entity), projectile);
        }
        return null;
    }
}
