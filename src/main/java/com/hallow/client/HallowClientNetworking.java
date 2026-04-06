package com.hallow.client;

import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.network.HallowInvSetPayload;
import com.hallow.network.HallowProtectionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class HallowClientNetworking {
    private HallowClientNetworking() {
    }

    public static boolean canUseHallowInvSync(Minecraft client) {
        if (client == null || client.player == null || client.hasSingleplayerServer()) {
            return false;
        }

        return serverSupportsHallowInv();
    }

    public static boolean serverSupportsHallowInv() {
        return ClientPlayNetworking.canSend(HallowInvSetPayload.TYPE);
    }

    public static boolean serverSupportsProtectionSync() {
        return ClientPlayNetworking.canSend(HallowProtectionPayload.TYPE);
    }

    public static boolean sendInventorySet(Minecraft client, int inventorySlot, ItemStack stack) {
        if (!canUseHallowInvSync(client)) {
            return false;
        }

        ClientPlayNetworking.send(new HallowInvSetPayload(inventorySlot, stack.copy()));
        return true;
    }

    public static void syncProtectionSettings(Minecraft client) {
        if (client == null || client.player == null || !serverSupportsProtectionSync()) {
            return;
        }

        HallowConfig.ProtectionSettings protection = HallowConfigManager.get().protection;
        ClientPlayNetworking.send(new HallowProtectionPayload(
            protection.invulnerable,
            protection.blockDrowningDamage,
            protection.blockFallDamage,
            protection.blockFreezeDamage,
            protection.blockFireDamage,
            protection.keepInventory,
            protection.blockPvpDamage
        ));
    }
}
