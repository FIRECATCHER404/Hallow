package com.hallow.client.cheat.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hallow.client.DirectionUtil;
import com.hallow.client.HallowRuntimeState;
import com.hallow.client.XRayRules;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class XRayModule extends CheatModule {
    private final List<HighlightTarget> targets = new ArrayList<>();
    private int scanCooldown;
    private String status = "X-Ray: idle";

    public XRayModule(int slot) {
        super("X-Ray", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) {
            targets.clear();
            status = "X-Ray: no world";
            return;
        }

        if (scanCooldown-- > 0) {
            return;
        }

        scanCooldown = HallowConfigManager.get().xray.scanInterval;
        scan(client);
    }

    @Override
    protected void onEnable(Minecraft client) {
        scanCooldown = 0;
        HallowRuntimeState.setXRayEnabled(client, true);
    }

    @Override
    protected void onDisable(Minecraft client) {
        scanCooldown = 0;
        targets.clear();
        status = "X-Ray: idle";
        HallowRuntimeState.setXRayEnabled(client, false);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of(status) : List.of();
    }

    public List<HighlightTarget> targets() {
        return List.copyOf(targets);
    }

    private void scan(Minecraft client) {
        targets.clear();
        HallowConfig.XRaySettings config = HallowConfigManager.get().xray;

        BlockPos center = BlockPos.containing(client.player.position());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Vec3 playerPos = client.player.position();

        for (int x = -config.horizontalRadius; x <= config.horizontalRadius; x++) {
            for (int y = -config.verticalRadius; y <= config.verticalRadius; y++) {
                for (int z = -config.horizontalRadius; z <= config.horizontalRadius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    BlockState state = client.level.getBlockState(cursor);
                    XRayRules.OreStyle style = XRayRules.styleFor(state);
                    if (style == null) {
                        continue;
                    }

                    double distanceSquared = Vec3.atCenterOf(cursor).distanceToSqr(playerPos);
                    targets.add(new HighlightTarget(cursor.immutable(), style.label(), style.red(), style.green(), style.blue(), distanceSquared));
                }
            }
        }

        targets.sort(Comparator.comparingDouble(HighlightTarget::distanceSquared));
        if (targets.size() > config.maxTargets) {
            targets.subList(config.maxTargets, targets.size()).clear();
        }

        if (targets.isEmpty()) {
            status = "X-Ray: " + config.mode.label() + " | no tracked block in " + config.horizontalRadius + "b";
            return;
        }

        HighlightTarget nearest = targets.getFirst();
        String heading = DirectionUtil.heading(client.player.position(), Vec3.atCenterOf(nearest.pos()));
        status = "X-Ray: "
            + config.mode.label()
            + " | "
            + targets.size()
            + " hits | nearest "
            + nearest.label()
            + " "
            + DirectionUtil.formatDistance(Math.sqrt(nearest.distanceSquared()))
            + " "
            + heading;
    }

    public record HighlightTarget(
        BlockPos pos,
        String label,
        float red,
        float green,
        float blue,
        double distanceSquared
    ) {
    }
}
