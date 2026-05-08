package net.minecraftforge.common;

import java.util.List;
import java.util.Objects;

public class ForgeConfigSpec {
    public static class Builder {
        public Builder push(String name) {
            return this;
        }

        public Builder pop() {
            return this;
        }

        public Builder comment(String comment) {
            return this;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return new BooleanValue(defaultValue);
        }

        public <T> ConfigValue<T> define(String name, T defaultValue) {
            return new ConfigValue<>(defaultValue);
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            return new IntValue(defaultValue);
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            return new DoubleValue(defaultValue);
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue) {
            return new EnumValue<>(defaultValue);
        }

        public ForgeConfigSpec build() {
            return new ForgeConfigSpec();
        }
    }

    public static class ConfigValue<T> {
        private T value;

        public ConfigValue(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = Objects.requireNonNull(value);
        }

        public void save() {
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        public BooleanValue(boolean value) {
            super(value);
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        public IntValue(int value) {
            super(value);
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        public DoubleValue(double value) {
            super(value);
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        public EnumValue(T value) {
            super(value);
        }
    }
}
