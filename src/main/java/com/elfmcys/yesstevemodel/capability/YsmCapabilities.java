package com.elfmcys.yesstevemodel.capability;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class YsmCapabilities {
    private static final Map<Object, Map<Capability<?>, Object>> STORE = new WeakHashMap<>();

    private YsmCapabilities() {
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> LazyOptional<T> get(Object target, Capability<T> capability) {
        if (target == null || capability == null) {
            return LazyOptional.empty();
        }
        Map<Capability<?>, Object> caps = STORE.computeIfAbsent(target, ignored -> new ConcurrentHashMap<>());
        Object value = caps.computeIfAbsent(capability, cap -> create(target, cap));
        return value == null ? LazyOptional.empty() : LazyOptional.of(() -> (T) value);
    }

    public static synchronized <T> void set(Object target, Capability<T> capability, T value) {
        if (target == null || capability == null || value == null) {
            return;
        }
        STORE.computeIfAbsent(target, ignored -> new ConcurrentHashMap<>()).put(capability, value);
    }

    private static Object create(Object target, Capability<?> capability) {
        if (target instanceof Player) {
            if (capability == ModelInfoCapabilityProvider.MODEL_INFO_CAP) {
                return new ModelInfoCapability();
            }
            if (capability == AuthModelsCapabilityProvider.AUTH_MODELS_CAP) {
                return new AuthModelsCapability();
            }
            if (capability == StarModelsCapabilityProvider.STAR_MODELS_CAP) {
                return new StarModelsCapability();
            }
        }
        if (target instanceof Projectile) {
            if (capability == ProjectileModelCapabilityProvider.PROJECTILE_MODEL) {
                return new ProjectileModelCapability();
            }
        }
        if (target instanceof Entity) {
            if (capability == VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP) {
                return new VehicleModelCapability();
            }
        }
        return createClientCapability(target, capability);
    }

    private static Object createClientCapability(Object target, Capability<?> capability) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return null;
        }
        return YsmClientCapabilities.create(target, capability);
    }

    @SuppressWarnings("unchecked")
    private static synchronized <T> LazyOptional<T> getExisting(Object target, Capability<T> capability) {
        Map<Capability<?>, Object> caps = STORE.get(target);
        if (caps == null) {
            return LazyOptional.empty();
        }
        Object value = caps.get(capability);
        return value == null ? LazyOptional.empty() : LazyOptional.of(() -> (T) value);
    }

    public static CompoundTag savePersistent(Entity entity) {
        CompoundTag root = new CompoundTag();
        getExisting(entity, ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> root.put("model_info", cap.serializeNBT()));
        getExisting(entity, ProjectileModelCapabilityProvider.PROJECTILE_MODEL).ifPresent(cap -> root.put("projectile_model", cap.serializeNBT()));
        getExisting(entity, VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).ifPresent(cap -> root.put("vehicle_model", cap.serializeNBT()));
        getExisting(entity, AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> root.put("auth_models", cap.serializeNBT()));
        getExisting(entity, StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> root.put("star_models", cap.serializeNBT()));
        return root;
    }

    public static void loadPersistent(Entity entity, CompoundTag root) {
        if (root == null || root.isEmpty()) {
            return;
        }
        if (root.contains("model_info")) {
            get(entity, ModelInfoCapabilityProvider.MODEL_INFO_CAP).ifPresent(cap -> cap.deserializeNBT(root.getCompound("model_info")));
        }
        if (root.contains("projectile_model")) {
            get(entity, ProjectileModelCapabilityProvider.PROJECTILE_MODEL).ifPresent(cap -> cap.deserializeNBT(root.getCompound("projectile_model")));
        }
        if (root.contains("vehicle_model")) {
            get(entity, VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).ifPresent(cap -> cap.deserializeNBT(root.getCompound("vehicle_model")));
        }
        if (root.contains("auth_models")) {
            get(entity, AuthModelsCapabilityProvider.AUTH_MODELS_CAP).ifPresent(cap -> cap.deserializeNBT((ListTag) root.get("auth_models")));
        }
        if (root.contains("star_models")) {
            get(entity, StarModelsCapabilityProvider.STAR_MODELS_CAP).ifPresent(cap -> cap.deserializeNBT((ListTag) root.get("star_models")));
        }
    }
}
