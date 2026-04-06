package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class NoPushModule extends CheatModule {
    public NoPushModule(int slot) {
        super("NoPush", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setNoPushEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setNoPushEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setNoPushEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("NoPush: entities + fluids") : List.of();
    }
}
