package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compatibility.YsmClientCompat;
import com.elfmcys.yesstevemodel.client.renderer.FirstPersonArmRenderHooks;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void ysm$renderCustomPlayer(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (player.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if ((!player.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get()) || player.isSpectator()) {
            return;
        }
        com.elfmcys.yesstevemodel.capability.YsmCapabilities.get(player, PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            if (cap.isModelActive()
                    && (!CameraUtil.isFirstPerson(cap)
                    || YsmClientCompat.isFirstPersonActive()
                    || YsmClientCompat.isRealCameraActive()
                    || GeneralConfig.DISABLE_EXTERNAL_FP_ANIM.get()
                    || !YsmClientCompat.isPlayerAnimatorAnimated(localPlayer))) {
                ci.cancel();
                RendererManager.getPlayerRenderer().render(player, player.getYRot(), partialTick, poseStack, bufferSource, packedLight);
            }
        });
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void ysm$renderRightHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (FirstPersonArmRenderHooks.renderCustomArm(player, HumanoidArm.RIGHT, poseStack, bufferSource, packedLight)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void ysm$renderLeftHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (FirstPersonArmRenderHooks.renderCustomArm(player, HumanoidArm.LEFT, poseStack, bufferSource, packedLight)) {
            ci.cancel();
        }
    }
}
