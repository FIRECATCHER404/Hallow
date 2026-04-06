package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.DirectionUtil;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

public final class LootCompassModule extends CheatModule {
    private int scanCooldown;
    private String status = "Loot Compass: idle";

    public LootCompassModule(int slot) {
        super("Loot Compass", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.player == null) {
            status = "Loot Compass: no player";
            return;
        }

        if (scanCooldown-- > 0) {
            return;
        }

        double searchRange = HallowConfigManager.get().lootCompass.range;
        scanCooldown = HallowConfigManager.get().lootCompass.scanInterval;

        AABB area = client.player.getBoundingBox().inflate(searchRange);
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (var entity : client.player.level().getEntities(
            client.player,
            area,
            candidate -> candidate instanceof ItemEntity item && !item.getItem().isEmpty()
        )) {
            ItemEntity itemEntity = (ItemEntity) entity;
            double distance = client.player.position().distanceTo(itemEntity.position());
            if (distance < nearestDistance) {
                nearest = itemEntity;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            status = "Loot Compass: no drops in " + (int) searchRange + "b";
            return;
        }

        String heading = DirectionUtil.heading(client.player.position(), nearest.position());
        status = "Loot Compass: "
            + nearest.getItem().getHoverName().getString()
            + " x"
            + nearest.getItem().getCount()
            + " "
            + DirectionUtil.formatDistance(nearestDistance)
            + " "
            + heading;
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of(status) : List.of();
    }

    @Override
    protected void onDisable(Minecraft client) {
        scanCooldown = 0;
        status = "Loot Compass: idle";
    }
}
