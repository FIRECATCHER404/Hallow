package com.hallow.client.cheat.modules;

import java.util.ArrayList;
import java.util.List;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;

public final class NoRenderModule extends CheatModule {
    public NoRenderModule(int slot) {
        super("NoRender", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setNoRenderEnabled(true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setNoRenderEnabled(false);
    }

    @Override
    public void reset(Minecraft client) {
        HallowRuntimeState.setNoRenderEnabled(false);
        super.reset(client);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        if (!isEnabled()) {
            return List.of();
        }

        HallowConfig.NoRenderSettings config = HallowConfigManager.get().noRender;
        List<String> parts = new ArrayList<>();
        if (config.hideFog) {
            parts.add("fog");
        }
        if (config.hideCameraOverlays) {
            parts.add("overlays");
        }
        if (config.hideViewBobbing) {
            parts.add("bob");
        }
        if (config.hideDamageTilt) {
            parts.add("tilt");
        }

        return List.of("NoRender: " + (parts.isEmpty() ? "idle" : String.join(", ", parts)));
    }

    @Override
    public String legendLine(Minecraft client) {
        return "=  NoRender" + (isEnabled() ? " [ON]" : " [OFF]");
    }
}
