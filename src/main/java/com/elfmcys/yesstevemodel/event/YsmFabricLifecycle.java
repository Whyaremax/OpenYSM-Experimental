package com.elfmcys.yesstevemodel.event;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class YsmFabricLifecycle {
    private static boolean commonRegistered;

    private YsmFabricLifecycle() {
    }

    public static void registerCommon() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        NetworkHandler.init();
        CommandRegistry.MODEL_IDS.toString();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommandRegistry.onRegisterCommand(new RegisterCommandsEvent(dispatcher, environment, registryAccess)));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            ServerStartupEvent.onServerAboutToStart(new ServerAboutToStartEvent(server));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerLifecycleHooks.setCurrentServer(null));
        ServerTickEvents.END_SERVER_TICK.register(server ->
                CapabilityEvent.onServerTick(new TickEvent.ServerTickEvent(server, TickEvent.Phase.END)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
                CapabilityEvent.onEntityJoinLevel(new EntityJoinLevelEvent(entity)));
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) ->
                CapabilityEvent.onStartTracking(new PlayerEvent.StartTracking(player, trackedEntity)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CapabilityEvent.onEntityJoinLevel(new EntityJoinLevelEvent(handler.player));
            EnterServerEvent.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(handler.player));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerLogoutEvent.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
    }

    public static void registerClient() {
        YsmFabricClientLifecycle.registerClient();
    }
}
