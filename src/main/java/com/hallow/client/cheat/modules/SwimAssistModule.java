package com.hallow.client.cheat.modules;

import java.util.List;
import java.util.Locale;

import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SwimAssistModule extends CheatModule {
    public SwimAssistModule(int slot) {
        super("Swim Assist", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || !player.isInWater()) {
            return;
        }

        Vec3 delta = player.getDeltaMovement();
        double horizontalBoost = HallowConfigManager.get().swimAssist.horizontalBoost;
        double verticalBoost = HallowConfigManager.get().swimAssist.verticalBoost;

        if (Math.abs(player.zza) > 0.01F || Math.abs(player.xxa) > 0.01F) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            double sin = Mth.sin(yaw);
            double cos = Mth.cos(yaw);
            double boostX = (-sin * player.zza + cos * player.xxa) * horizontalBoost;
            double boostZ = (cos * player.zza + sin * player.xxa) * horizontalBoost;
            delta = delta.add(boostX, 0.0, boostZ);
        }

        if (client.options.keyJump.isDown()) {
            delta = delta.add(0.0, verticalBoost, 0.0);
        } else if (client.options.keyShift.isDown()) {
            delta = delta.add(0.0, -verticalBoost, 0.0);
        }

        player.setDeltaMovement(delta);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled()
            ? List.of("Swim Assist: " + String.format(Locale.ROOT, "%.2f", HallowConfigManager.get().swimAssist.horizontalBoost) + " boost")
            : List.of();
    }
}
