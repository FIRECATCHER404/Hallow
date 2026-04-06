package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.screen.CreativeAccessScreen;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class ChestStealerModule extends CheatModule {
    private static final int STEAL_DELAY_TICKS = 2;

    private int stealCooldown;

    public ChestStealerModule(int slot) {
        super("Chest Stealer", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.gameMode == null) {
            stealCooldown = 0;
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen<?> screen) || client.screen instanceof InventoryScreen || client.screen instanceof CreativeAccessScreen) {
            stealCooldown = 0;
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        if (!hasExternalInventory(menu, client)) {
            stealCooldown = 0;
            return;
        }

        if (stealCooldown-- > 0) {
            return;
        }

        for (Slot slot : menu.slots) {
            if (slot.container == client.player.getInventory() || !slot.hasItem() || !slot.mayPickup(client.player)) {
                continue;
            }

            client.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, client.player);
            stealCooldown = STEAL_DELAY_TICKS;
            return;
        }

        client.player.closeContainer();
    }

    @Override
    protected void onDisable(Minecraft client) {
        stealCooldown = 0;
    }

    @Override
    public void reset(Minecraft client) {
        stealCooldown = 0;
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("Chest Stealer: live") : List.of();
    }

    private boolean hasExternalInventory(AbstractContainerMenu menu, Minecraft client) {
        return menu.slots.stream().anyMatch(slot -> slot.container != client.player.getInventory());
    }
}
