package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoSprintModule extends CheatModule {
    public AutoSprintModule(int slot) {
        super("Auto Sprint", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || player.isShiftKeyDown() || player.isSwimming()) {
            return;
        }

        if (player.zza <= 0.8F || player.horizontalCollision) {
            return;
        }

        if (!HallowConfigManager.get().autoSprint.keepWhileUsingItem && player.isUsingItem()) {
            return;
        }

        if (player.canSprint()) {
            player.setSprinting(true);
        }
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("Auto Sprint: live") : List.of();
    }
}
