package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class RenderHandEvent {
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final float partialTick;
    private final int packedLight;

    public RenderHandEvent(PoseStack poseStack, MultiBufferSource multiBufferSource, float partialTick, int packedLight) {
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.partialTick = partialTick;
        this.packedLight = packedLight;
    }

    public PoseStack getPoseStack() { return poseStack; }
    public MultiBufferSource getMultiBufferSource() { return multiBufferSource; }
    public float getPartialTick() { return partialTick; }
    public int getPackedLight() { return packedLight; }
}
