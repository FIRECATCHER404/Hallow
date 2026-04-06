package com.hallow;

import com.hallow.network.HallowNetworking;
import com.hallow.protection.HallowProtectionManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class Hallow implements ModInitializer {
    @Override
    public void onInitialize() {
        HallowNetworking.initialize();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HallowProtectionManager.remove(handler.getPlayer().getUUID()));
    }
}
