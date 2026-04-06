package com.hallow.network;

import com.hallow.protection.HallowProtectionManager;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HallowNetworking {
    public static final int OFFHAND_SLOT = 40;

    private static boolean initialized;

    private HallowNetworking() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        PayloadTypeRegistry.playC2S().register(HallowInvSetPayload.TYPE, HallowInvSetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HallowProtectionPayload.TYPE, HallowProtectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HallowInvSetPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            int inventorySlot = payload.inventorySlot();
            if (!isSupportedInventorySlot(inventorySlot)) {
                return;
            }

            ItemStack stack = payload.stack().copy();
            if (!stack.isEmpty()) {
                if (!stack.isItemEnabled(player.level().enabledFeatures())) {
                    return;
                }

                if (stack.getCount() > stack.getMaxStackSize()) {
                    stack.setCount(stack.getMaxStackSize());
                }
            }

            applyInventorySet(player, inventorySlot, stack);
        }));
        ServerPlayNetworking.registerGlobalReceiver(HallowProtectionPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            HallowProtectionManager.update(context.player(), payload.asState());
        }));
    }

    public static void applyInventorySet(ServerPlayer player, int inventorySlot, ItemStack stack) {
        int menuSlot = toMenuSlot(inventorySlot);
        ItemStack syncedStack = stack.copy();

        player.getInventory().setItem(inventorySlot, syncedStack.copy());
        player.inventoryMenu.getSlot(menuSlot).setByPlayer(syncedStack.copy());
        player.inventoryMenu.setRemoteSlot(menuSlot, syncedStack.copy());
        player.inventoryMenu.broadcastChanges();

        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static int toMenuSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return 36 + inventorySlot;
        }

        if (inventorySlot == OFFHAND_SLOT) {
            return 45;
        }

        return inventorySlot;
    }

    public static boolean isSupportedInventorySlot(int inventorySlot) {
        return (inventorySlot >= 0 && inventorySlot < 36) || inventorySlot == OFFHAND_SLOT;
    }
}
