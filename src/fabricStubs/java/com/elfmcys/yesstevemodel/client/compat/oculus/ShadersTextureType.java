package com.elfmcys.yesstevemodel.client.compat.oculus;

import net.minecraft.resources.ResourceLocation;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String suffix) {
        this.suffix = suffix;
    }

    public ResourceLocation appendSuffix(ResourceLocation location) {
        return new ResourceLocation(location.getNamespace(), location.getPath() + suffix);
    }
}
