package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

public class SophisticatedBackpackLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final EntityModel<Player> backpackModel = createBackpackModel();

    public SophisticatedBackpackLayer() {
    }

    private static EntityModel<Player> createBackpackModel() {
        return new EntityModel<>() {
            public void setupAnim(Player player, float f, float f2, float f3, float f4, float f5) {
            }

            public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i2, float f, float f2, float f3, float f4) {
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderBackpack(PoseStack poseStack, AnimatedGeoModel model) {
        RenderUtils.prepMatrixForLocator(poseStack, model.backpackBones());
    }
}
