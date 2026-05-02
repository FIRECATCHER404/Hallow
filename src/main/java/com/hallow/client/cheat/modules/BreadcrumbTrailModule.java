package com.hallow.client.cheat.modules;

import java.util.ArrayList;
import java.util.List;

import com.hallow.client.DirectionUtil;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class BreadcrumbTrailModule extends CheatModule {
    private final List<Vec3> trail = new ArrayList<>();
    private Identifier dimension;
    private Vec3 lastRecorded;

    public BreadcrumbTrailModule(int slot) {
        super("Breadcrumb Trail", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) {
            clearTrail();
            return;
        }

        Identifier currentDimension = client.level.dimension().identifier();
        if (dimension == null || !dimension.equals(currentDimension)) {
            clearTrail();
            dimension = currentDimension;
        }

        Vec3 currentPosition = client.player.position();
        double sampleDistance = HallowConfigManager.get().breadcrumbTrail.sampleDistance;
        if (lastRecorded == null || currentPosition.distanceTo(lastRecorded) >= sampleDistance) {
            trail.add(currentPosition);
            lastRecorded = currentPosition;
            trimTrail();
        }
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        if (!isEnabled()) {
            return List.of();
        }

        double distance = 0.0;
        for (int index = 0; index < trail.size() - 1; index++) {
            distance += trail.get(index).distanceTo(trail.get(index + 1));
        }

        return List.of("Breadcrumb Trail: " + trail.size() + " points | " + DirectionUtil.formatDistance(distance));
    }

    @Override
    protected void onEnable(Minecraft client) {
        clearTrail();
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearTrail();
    }

    @Override
    public void reset(Minecraft client) {
        super.reset(client);
        clearTrail();
    }

    public List<Vec3> trailPoints() {
        return trail;
    }

    private void trimTrail() {
        int maxPoints = HallowConfigManager.get().breadcrumbTrail.maxPoints;
        while (trail.size() > maxPoints) {
            trail.removeFirst();
        }
    }

    private void clearTrail() {
        trail.clear();
        dimension = null;
        lastRecorded = null;
    }
}
