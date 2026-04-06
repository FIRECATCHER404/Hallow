package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.ProjectilePredictor;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;

public final class ProjectilePredictModule extends CheatModule {
    public ProjectilePredictModule(int slot) {
        super("Projectile Predict", slot, true);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        if (!isEnabled()) {
            return List.of();
        }

        String state = ProjectilePredictor.describeHeldProjectile(client);
        return List.of("Projectile Predict: " + state);
    }

    @Override
    public String legendLine(Minecraft client) {
        return "-  Projectile Predict" + (isEnabled() ? " [ON]" : " [OFF]");
    }
}
