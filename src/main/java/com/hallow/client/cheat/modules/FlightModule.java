package com.hallow.client.cheat.modules;

import java.util.List;
import java.util.Locale;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FlightModule extends CheatModule {
    private boolean storedMayfly;
    private boolean storedFlying;
    private float storedFlyingSpeed;
    private boolean capturedState;

    public FlightModule(int slot) {
        super("Fly", slot, true);
    }

    @Override
    protected void onEnable(Minecraft client) {
        HallowRuntimeState.setFlightEnabled(true);
        applyFlight(client);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        applyFlight(client);
    }

    @Override
    protected void onDisable(Minecraft client) {
        HallowRuntimeState.setFlightEnabled(false);
        LocalPlayer player = client.player;
        if (player == null) {
            capturedState = false;
            return;
        }

        if (!player.isCreative() && !player.isSpectator() && capturedState) {
            player.getAbilities().mayfly = storedMayfly;
            player.getAbilities().flying = storedFlying;
        }

        if (capturedState) {
            player.getAbilities().setFlyingSpeed(storedFlyingSpeed);
            player.onUpdateAbilities();
        }

        capturedState = false;
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled()
            ? List.of("Fly: speed " + String.format(Locale.ROOT, "%.2f", HallowConfigManager.get().fly.speed))
            : List.of();
    }

    private void applyFlight(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (!capturedState) {
            storedMayfly = player.getAbilities().mayfly;
            storedFlying = player.getAbilities().flying;
            storedFlyingSpeed = player.getAbilities().getFlyingSpeed();
            capturedState = true;
        }

        if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
        }

        player.resetFallDistance();
        player.getAbilities().setFlyingSpeed((float) HallowConfigManager.get().fly.speed);
        player.onUpdateAbilities();
    }
}
