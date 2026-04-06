package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class SafeWalkModule extends CheatModule {
    public SafeWalkModule(int slot) {
        super("SafeWalk", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setSafeWalkEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setSafeWalkEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setSafeWalkEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("SafeWalk: ledges locked") : List.of();
    }
}
