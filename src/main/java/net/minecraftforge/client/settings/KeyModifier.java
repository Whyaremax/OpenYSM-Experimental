package net.minecraftforge.client.settings;

public enum KeyModifier {
    NONE,
    ALT,
    CONTROL,
    SHIFT;

    public static KeyModifier getActiveModifier() {
        return NONE;
    }
}
