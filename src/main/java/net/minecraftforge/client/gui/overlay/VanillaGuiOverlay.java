package net.minecraftforge.client.gui.overlay;

import net.minecraft.resources.ResourceLocation;

public enum VanillaGuiOverlay {
    DEBUG_TEXT;

    public ResourceLocation id() {
        return new ResourceLocation("minecraft", name().toLowerCase(java.util.Locale.ROOT));
    }
}
