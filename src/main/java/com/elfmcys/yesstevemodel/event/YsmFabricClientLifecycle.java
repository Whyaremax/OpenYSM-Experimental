package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.client.event.ClientCommonInit;
import com.elfmcys.yesstevemodel.client.event.ClientPlayerJoinNotification;
import com.elfmcys.yesstevemodel.client.event.ClientSetupEvent;
import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.LoadingStateOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class YsmFabricClientLifecycle {
    private static boolean clientRegistered;
    private static boolean setupWorkDrained;
    private static final ForgeGui FORGE_GUI = new ForgeGui();
    private static final LoadingStateOverlay EXTRA_PLAYER_OVERLAY = new LoadingStateOverlay();
    private static final ModelSyncStateOverlay MODEL_SYNC_OVERLAY = new ModelSyncStateOverlay();

    private YsmFabricClientLifecycle() {
    }

    static void registerClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        ClientSetupEvent.onClientSetup(new net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent());
        ClientCommonInit.run();
        RegisterKeyMappingsEvent keyEvent = new RegisterKeyMappingsEvent();
        ClientSetupEvent.onRegisterKeyMappings(keyEvent);

        ClientTickEvents.START_CLIENT_TICK.register(client -> drainClientSetupWork());
        ClientTickEvents.START_CLIENT_TICK.register(client -> tickKeys());
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                ClientTickEvent.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayerJoinNotification.onPlayerLogin(new net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientPlayerJoinNotification.onPlayerLogout(new net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut()));
        HudRenderCallback.EVENT.register(YsmFabricClientLifecycle::renderHud);
    }

    private static void drainClientSetupWork() {
        if (setupWorkDrained) {
            return;
        }
        setupWorkDrained = true;
        FMLClientSetupEvent.runQueuedWork();
    }

    private static void tickKeys() {
        fireKey(PlayerModelToggleKey.KEY_MAPPING, PlayerModelToggleKey::onKeyInput);
        fireKey(AnimationRouletteKey.KEY_ROULETTE, AnimationRouletteKey::onKeyInput);
        fireKey(DebugAnimationKey.KEY_MAPPING, DebugAnimationKey::onKeyInput);
        fireKey(ExtraPlayerRenderKey.KEY_MAPPING, ExtraPlayerRenderKey::onKeyInput);
        for (KeyMapping mapping : ExtraAnimationKey.KEY_MAPPINGS) {
            fireKey(mapping, ExtraAnimationKey::onKeyInput);
        }
    }

    private static void fireKey(KeyMapping mapping, java.util.function.Consumer<InputEvent.Key> handler) {
        while (mapping.consumeClick()) {
            handler.accept(new InputEvent.Key(mapping.getDefaultKey().getValue(), 0, 1));
        }
    }

    private static void renderHud(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        AnimationDebugOverlay.createOverlay().render(FORGE_GUI, guiGraphics, tickDelta, width, height);
        EXTRA_PLAYER_OVERLAY.render(FORGE_GUI, guiGraphics, tickDelta, width, height);
        MODEL_SYNC_OVERLAY.render(FORGE_GUI, guiGraphics, tickDelta, width, height);
    }
}
