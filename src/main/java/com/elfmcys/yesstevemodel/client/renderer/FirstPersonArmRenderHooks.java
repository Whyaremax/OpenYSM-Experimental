package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

public final class FirstPersonArmRenderHooks {
    private FirstPersonArmRenderHooks() {
    }

    public static boolean renderCustomArm(AbstractClientPlayer player, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!shouldRenderFirstPersonArms() || !(player instanceof LocalPlayer localPlayer)) {
            return false;
        }
        return com.elfmcys.yesstevemodel.capability.YsmCapabilities.get(localPlayer, PlayerCapabilityProvider.PLAYER_CAP).map(cap -> {
            if (!cap.isModelActive()) {
                return false;
            }
            ModelAssembly context = cap.getModelAssembly();
            if (context == null || !hasArmBone(arm, context.getAnimationBundle().getArmModel())) {
                return false;
            }
            RendererManager.getHandRenderer().renderHandItem(localPlayer, context, cap, arm, poseStack, bufferSource, packedLight, Minecraft.getInstance().getFrameTime());
            return true;
        }).orElse(false);
    }

    public static void renderCustomArmBackground(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (!shouldRenderFirstPersonArms()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        com.elfmcys.yesstevemodel.capability.YsmCapabilities.get(player, PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            if (!cap.isModelActive()) {
                return;
            }
            ModelAssembly modelAssembly = cap.getModelAssembly();
            if (modelAssembly == null || !modelAssembly.getAnimationBundle().getArmModel().hasCustomLimbs) {
                return;
            }
            if (MinecraftForge.EVENT_BUS.post(new SpecialPlayerRenderEvent(player, cap, cap.getModelId()))) {
                return;
            }
            ResourceLocation texture = cap.getTextureLocation();
            int textureIndex = cap.getTextureIndex();
            VertexConsumer buffer = bufferSource.getBuffer(CustomEntityTranslucentRenderType.get(texture));
            poseStack.pushPose();
            if (Minecraft.getInstance().options.bobView().get()) {
                applyHandTransform(poseStack, partialTick, player);
            }
            poseStack.translate(0.0d, -1.5d, 0.0d);
            NativeModelRenderer.renderMesh(buffer, poseStack.last(), modelAssembly.getAnimationBundle().getArmModel(), modelAssembly.getAnimationBundle().getArmModel().getBoneTransformData(), null, textureIndex, 3, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        });
    }

    private static boolean shouldRenderFirstPersonArms() {
        return YesSteveModel.isAvailable() && !GeneralConfig.DISABLE_SELF_MODEL.get() && !GeneralConfig.DISABLE_SELF_HANDS.get();
    }

    private static boolean hasArmBone(HumanoidArm humanoidArm, GeoModel meshData) {
        if (humanoidArm == HumanoidArm.LEFT) {
            return meshData.hasCustomLeftHand;
        }
        return meshData.hasCustomRightHand;
    }

    private static void applyHandTransform(PoseStack poseStack, float partialTick, Player player) {
        float walk = -(player.walkDist + ((player.walkDist - player.walkDistO) * partialTick));
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        poseStack.translate((-Mth.sin(walk * 3.1415927f)) * bob * 0.5f, Math.abs(Mth.cos(walk * 3.1415927f) * bob), 0.0d);
        poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(Mth.sin(walk * 3.1415927f) * bob * 3.0f));
        poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees(Math.abs(Mth.cos((walk * 3.1415927f) - 0.2f) * bob) * 5.0f));
    }
}
