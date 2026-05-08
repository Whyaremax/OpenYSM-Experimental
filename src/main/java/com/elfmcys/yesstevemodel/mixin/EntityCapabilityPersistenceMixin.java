package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.capability.YsmCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCapabilityPersistenceMixin {
    private static final String KEY = "openysm_fabric_caps";

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void openysm$saveCaps(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag caps = YsmCapabilities.savePersistent((Entity) (Object) this);
        if (!caps.isEmpty()) {
            tag.put(KEY, caps);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void openysm$loadCaps(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(KEY)) {
            YsmCapabilities.loadPersistent((Entity) (Object) this, tag.getCompound(KEY));
        }
    }
}
