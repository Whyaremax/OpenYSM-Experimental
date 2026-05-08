package com.elfmcys.yesstevemodel.config;

import java.util.Locale;

public enum NativeAccelerationMode {
    AUTO,
    OFF,
    RENDER_ONLY,
    FULL_EXPERIMENTAL;

    public boolean allowsRenderer() {
        return this == RENDER_ONLY || this == FULL_EXPERIMENTAL;
    }

    public static NativeAccelerationMode parse(String value, NativeAccelerationMode fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return NativeAccelerationMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
