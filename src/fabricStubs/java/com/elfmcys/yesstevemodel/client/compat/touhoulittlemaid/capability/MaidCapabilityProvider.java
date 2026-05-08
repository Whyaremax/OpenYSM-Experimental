package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class MaidCapabilityProvider {
    public static final Capability<Object> MAID_CAP = CapabilityManager.get(new CapabilityToken<Object>() {
    });

    private MaidCapabilityProvider() {
    }
}
