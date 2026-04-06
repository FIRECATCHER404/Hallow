package com.hallow.client.cheat.modules;

import java.util.List;
import java.util.UUID;

import com.hallow.client.HallowClientNetworking;
import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.screen.CreativeAccessScreen;
import com.hallow.network.HallowNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CreativeAccessModule extends CheatModule {
    private boolean suppressOpenOnEnable;

    public CreativeAccessModule(int slot) {
        super("HallowInv", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setCreativeAccessEnabled(client, true);
        if (!suppressOpenOnEnable && HallowConfigManager.get().creativeAccess.openOnEnable) {
            openPickerScreen(client, null);
        }
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client.player == null) {
            return;
        }

        HallowRuntimeState.setCreativeAccessEnabled(client, true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setCreativeAccessEnabled(client, false);
        if (client.player != null && client.screen instanceof CreativeAccessScreen) {
            client.setScreen(new InventoryScreen(client.player));
        }
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setCreativeAccessEnabled(client, false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("HallowInv: live") : List.of();
    }

    public void openFromShortcut(Minecraft client) {
        if (!isEnabled()) {
            setEnabled(client, true);
        }

        openPickerScreen(client, null);
    }

    public void restoreEnabledState(Minecraft client, boolean enabled) {
        suppressOpenOnEnable = true;
        try {
            setEnabled(client, enabled);
        } finally {
            suppressOpenOnEnable = false;
        }
    }

    public void assignItemToInventory(Minecraft client, Item item, int requestedCount, int inventorySlot) {
        if (item == null) {
            return;
        }

        assignStackToInventory(client, new ItemStack(item, Math.min(requestedCount, new ItemStack(item).getMaxStackSize())), inventorySlot);
    }

    public void assignStackToInventory(Minecraft client, ItemStack stack, int inventorySlot) {
        LocalPlayer player = client.player;
        if (player == null || !HallowNetworking.isSupportedInventorySlot(inventorySlot)) {
            return;
        }

        ItemStack sanitized = sanitizeStack(stack);
        int menuSlot = HallowNetworking.toMenuSlot(inventorySlot);
        applyClientSlot(player, inventorySlot, menuSlot, sanitized.copy());
        syncInventorySlot(client, player.getUUID(), inventorySlot, sanitized.copy());
    }

    public void clearInventorySlot(Minecraft client, int inventorySlot) {
        assignStackToInventory(client, ItemStack.EMPTY, inventorySlot);
    }

    private void openPickerScreen(Minecraft client, net.minecraft.client.gui.screens.Screen previous) {
        if (client.player == null) {
            return;
        }

        client.setScreen(new CreativeAccessScreen(previous, this));
    }

    private void applyClientSlot(LocalPlayer player, int inventorySlot, int serverSlot, ItemStack stack) {
        ItemStack clientCopy = stack.copy();
        player.getInventory().setItem(inventorySlot, clientCopy.copy());
        player.inventoryMenu.getSlot(serverSlot).setByPlayer(clientCopy.copy());
        player.inventoryMenu.setRemoteSlot(serverSlot, clientCopy.copy());
        player.inventoryMenu.broadcastChanges();
    }

    private void syncInventorySlot(Minecraft client, UUID playerId, int inventorySlot, ItemStack stack) {
        if (HallowClientNetworking.sendInventorySet(client, inventorySlot, stack)) {
            return;
        }

        if (!client.hasSingleplayerServer()) {
            return;
        }

        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) {
            return;
        }

        ItemStack syncedStack = stack.copy();
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
            if (serverPlayer != null) {
                HallowNetworking.applyInventorySet(serverPlayer, inventorySlot, syncedStack.copy());
            }
        });
    }

    private ItemStack sanitizeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        if (copy.getCount() > copy.getMaxStackSize()) {
            copy.setCount(copy.getMaxStackSize());
        }
        return copy;
    }
}
