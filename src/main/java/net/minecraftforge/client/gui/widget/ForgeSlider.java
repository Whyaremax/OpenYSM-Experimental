package net.minecraftforge.client.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.text.DecimalFormat;

public class ForgeSlider extends AbstractSliderButton {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    protected final Component prefix;
    protected final Component suffix;
    protected final double minValue;
    protected final double maxValue;
    protected final double stepSize;
    protected final int precision;
    protected final boolean drawString;

    public ForgeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, boolean drawString) {
        this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, 1.0d, 2, drawString);
    }

    public ForgeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, double stepSize, int precision, boolean drawString) {
        super(x, y, width, height, Component.empty(), normalize(minValue, maxValue, currentValue));
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize;
        this.precision = precision;
        this.drawString = drawString;
        updateMessage();
    }

    private static double normalize(double minValue, double maxValue, double currentValue) {
        if (maxValue <= minValue) {
            return 0.0d;
        }
        return Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0d, 1.0d);
    }

    public double getValue() {
        double raw = minValue + (maxValue - minValue) * value;
        if (stepSize > 0.0d) {
            raw = minValue + Math.round((raw - minValue) / stepSize) * stepSize;
        }
        return Mth.clamp(raw, minValue, maxValue);
    }

    @Override
    protected void updateMessage() {
        if (!drawString) {
            setMessage(Component.empty());
            return;
        }
        setMessage(Component.empty().append(prefix).append(": ").append(FORMAT.format(getValue())).append(suffix));
    }

    @Override
    protected void applyValue() {
    }

    public int getFGColor() {
        return active ? 0xFFFFFF : 0xA0A0A0;
    }

    protected int getTextureY() {
        return isHoveredOrFocused() ? 20 : 0;
    }

    protected int getHandleTextureY() {
        return getTextureY();
    }
}
