package com.hallow.client.cheat.modules;

import java.util.List;
import java.util.Locale;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;

public final class StepAssistModule extends CheatModule {
    public StepAssistModule(int slot) {
        super("Step Assist", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setStepAssistEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setStepAssistEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setStepAssistEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled()
            ? List.of("Step Assist: " + String.format(Locale.ROOT, "%.2f", HallowConfigManager.get().stepAssist.height) + " blocks")
            : List.of();
    }
}
