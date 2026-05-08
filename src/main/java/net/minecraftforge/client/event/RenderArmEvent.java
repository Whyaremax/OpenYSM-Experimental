package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class RenderArmEvent extends Event {
    private final Player player;
    private final HumanoidArm arm;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;

    public RenderArmEvent(Player player, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        this.player = player;
        this.arm = arm;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
    }

    public Player getPlayer() { return player; }
    public HumanoidArm getArm() { return arm; }
    public PoseStack getPoseStack() { return poseStack; }
    public MultiBufferSource getMultiBufferSource() { return multiBufferSource; }
    public int getPackedLight() { return packedLight; }
}
