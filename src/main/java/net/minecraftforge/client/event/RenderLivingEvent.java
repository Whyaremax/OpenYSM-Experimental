package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public class RenderLivingEvent<T extends LivingEntity, M> extends Event {
    private final T entity;
    private final LivingEntityRenderer<T, ?> renderer;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;
    private final float partialTick;

    public RenderLivingEvent(T entity, LivingEntityRenderer<T, ?> renderer, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, float partialTick) {
        this.entity = entity;
        this.renderer = renderer;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
        this.partialTick = partialTick;
    }

    public T getEntity() { return entity; }
    public LivingEntityRenderer<T, ?> getRenderer() { return renderer; }
    public PoseStack getPoseStack() { return poseStack; }
    public MultiBufferSource getMultiBufferSource() { return multiBufferSource; }
    public int getPackedLight() { return packedLight; }
    public float getPartialTick() { return partialTick; }

    public static class Pre<T extends LivingEntity, M> extends RenderLivingEvent<T, M> {
        public Pre(T entity, LivingEntityRenderer<T, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
            super(entity, renderer, poseStack, multiBufferSource, packedLight, partialTick);
        }
    }

    public static class Post<T extends LivingEntity, M> extends RenderLivingEvent<T, M> {
        public Post(T entity, LivingEntityRenderer<T, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
            super(entity, renderer, poseStack, multiBufferSource, packedLight, partialTick);
        }
    }
}
