package net.minecraftforge.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public class ForgeGui {
    public Font getFont() {
        return Minecraft.getInstance().font;
    }
}
