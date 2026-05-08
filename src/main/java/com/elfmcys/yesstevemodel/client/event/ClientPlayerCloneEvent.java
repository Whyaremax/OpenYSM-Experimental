package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class ClientPlayerCloneEvent {
    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        com.elfmcys.yesstevemodel.capability.YsmCapabilities.get(event.getOldPlayer(), PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> com.elfmcys.yesstevemodel.capability.YsmCapabilities.get(event.getNewPlayer(), PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap2 -> cap2.copyFrom(cap)));
    }
}
