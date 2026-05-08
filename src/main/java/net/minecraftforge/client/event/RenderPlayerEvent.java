package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class RenderPlayerEvent extends Event {
    private final Player entity;
    private final float partialTick;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;

    public RenderPlayerEvent(Player entity, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        this.entity = entity;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
    }

    public Player getEntity() { return entity; }
    public float getPartialTick() { return partialTick; }
    public PoseStack getPoseStack() { return poseStack; }
    public MultiBufferSource getMultiBufferSource() { return multiBufferSource; }
    public int getPackedLight() { return packedLight; }

    public static class Pre extends RenderPlayerEvent {
        public Pre(Player entity, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
            super(entity, partialTick, poseStack, multiBufferSource, packedLight);
        }
    }
}
