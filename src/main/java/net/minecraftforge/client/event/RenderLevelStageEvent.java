package net.minecraftforge.client.event;

public class RenderLevelStageEvent {
    private final Stage stage;

    public RenderLevelStageEvent(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public enum Stage {
        AFTER_CUTOUT_BLOCKS
    }
}
