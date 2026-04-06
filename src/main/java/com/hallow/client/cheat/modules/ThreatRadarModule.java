package com.hallow.client.cheat.modules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hallow.client.DirectionUtil;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ThreatRadarModule extends CheatModule {
    private final Set<Integer> highlightedEntityIds = new HashSet<>();
    private int scanCooldown;
    private String status = "Threat Radar: idle";

    public ThreatRadarModule(int slot) {
        super("Threat Radar", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) {
            clearHighlights(client);
            status = "Threat Radar: no player";
            return;
        }

        if (scanCooldown-- > 0) {
            return;
        }

        double searchRange = HallowConfigManager.get().threatRadar.range;
        double blindsideThreshold = HallowConfigManager.get().threatRadar.blindsideThreshold;
        boolean highlightPlayers = HallowConfigManager.get().threatRadar.highlightPlayers;
        scanCooldown = HallowConfigManager.get().threatRadar.scanInterval;

        AABB area = client.player.getBoundingBox().inflate(searchRange);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int blindsideCount = 0;
        Vec3 look = client.player.getLookAngle().normalize();
        Set<Integer> visibleThreatIds = new HashSet<>();

        for (var entity : client.player.level().getEntities(client.player, area, candidate -> isTrackedThreat(candidate, client.player, highlightPlayers))) {
            LivingEntity living = (LivingEntity) entity;
            visibleThreatIds.add(living.getId());
            if (!highlightedEntityIds.contains(living.getId()) && !living.hasGlowingTag()) {
                living.setGlowingTag(true);
                highlightedEntityIds.add(living.getId());
            }

            Vec3 offset = living.position().subtract(client.player.position());
            double distance = offset.length();

            if (distance < nearestDistance) {
                nearest = living;
                nearestDistance = distance;
            }

            if (distance > 0.001 && look.dot(offset.normalize()) < blindsideThreshold) {
                blindsideCount++;
            }
        }

        clearMissingHighlights(client, visibleThreatIds);

        if (nearest == null) {
            status = "Threat Radar: clear";
            return;
        }

        String heading = DirectionUtil.heading(client.player.position(), nearest.position());
        status = "Threat Radar: "
            + nearest.getName().getString()
            + " "
            + DirectionUtil.formatDistance(nearestDistance)
            + " "
            + heading
            + " | blindside "
            + blindsideCount;
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of(status) : List.of();
    }

    @Override
    protected void onDisable(Minecraft client) {
        clearHighlights(client);
        scanCooldown = 0;
        status = "Threat Radar: idle";
    }

    @Override
    public void reset(Minecraft client) {
        clearHighlights(client);
        scanCooldown = 0;
        status = "Threat Radar: idle";
    }

    private void clearMissingHighlights(Minecraft client, Set<Integer> visibleThreatIds) {
        highlightedEntityIds.removeIf(entityId -> {
            if (visibleThreatIds.contains(entityId)) {
                return false;
            }

            Entity entity = client.level != null ? client.level.getEntity(entityId) : null;
            if (entity instanceof LivingEntity living) {
                living.setGlowingTag(false);
            }

            return true;
        });
    }

    private void clearHighlights(Minecraft client) {
        if (client.level != null) {
            for (int entityId : highlightedEntityIds) {
                Entity entity = client.level.getEntity(entityId);
                if (entity instanceof LivingEntity living) {
                    living.setGlowingTag(false);
                }
            }
        }

        highlightedEntityIds.clear();
    }

    private boolean isTrackedThreat(Entity candidate, Player localPlayer, boolean highlightPlayers) {
        if (!(candidate instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }

        if (candidate instanceof Enemy) {
            return true;
        }

        return highlightPlayers && candidate instanceof Player player && player != localPlayer;
    }
}
