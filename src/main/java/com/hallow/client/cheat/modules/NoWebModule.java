package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class NoWebModule extends CheatModule {
    public NoWebModule(int slot) {
        super("NoWeb", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setNoWebEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setNoWebEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setNoWebEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("NoWeb: cobwebs ignored") : List.of();
    }
}
