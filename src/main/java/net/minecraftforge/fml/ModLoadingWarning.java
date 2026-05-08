package net.minecraftforge.fml;

public record ModLoadingWarning(Object modInfo, ModLoadingStage stage, String key, Object... args) {
}
