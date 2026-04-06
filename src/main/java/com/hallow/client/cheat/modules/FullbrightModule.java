package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class FullbrightModule extends CheatModule {
    public FullbrightModule(int slot) {
        super("Fullbright", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setFullbrightEnabled(true);
    }

    @Override
    public void tick(Minecraft client) {
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setFullbrightEnabled(false);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled()
            ? List.of("Fullbright: night vision")
            : List.of();
    }
}
