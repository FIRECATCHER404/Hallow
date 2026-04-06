package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class NoSlowModule extends CheatModule {
    public NoSlowModule(int slot) {
        super("NoSlow", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setNoSlowEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setNoSlowEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setNoSlowEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("NoSlow: item use") : List.of();
    }
}
