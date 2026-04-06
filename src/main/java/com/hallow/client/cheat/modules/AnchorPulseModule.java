package com.hallow.client.cheat.modules;

import java.util.List;
import java.util.Locale;

import com.hallow.client.DirectionUtil;
import com.hallow.client.cheat.CheatModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.HallowProfileState;
import com.hallow.client.config.HallowStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class AnchorPulseModule extends CheatModule {
    private Vec3 anchor;
    private Identifier anchorDimension;

    public AnchorPulseModule(int slot) {
        super("Anchor Pulse", slot, false);
    }

    @Override
    protected void activate(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        if (client.player.isShiftKeyDown()) {
            anchor = null;
            anchorDimension = null;
            HallowStorage.markDirty();
            announce(client, "Anchor Pulse cleared.");
            return;
        }

        anchor = client.player.position();
        anchorDimension = client.level.dimension().identifier();
        announce(
            client,
            String.format(
                Locale.ROOT,
                "Anchor saved at %.1f, %.1f, %.1f.",
                anchor.x,
                anchor.y,
                anchor.z
            )
        );
        HallowStorage.markDirty();
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        if (anchor == null || client.player == null || client.level == null) {
            return List.of();
        }

        if (!client.level.dimension().identifier().equals(anchorDimension)) {
            return List.of("Anchor Pulse: stored in " + anchorDimension);
        }

        Vec3 playerPos = client.player.position();
        double distance = playerPos.distanceTo(anchor);
        String heading = DirectionUtil.heading(playerPos, anchor);
        String vertical = DirectionUtil.verticalOffset(anchor.y - playerPos.y);
        String baseLine = "Anchor Pulse: " + DirectionUtil.formatDistance(distance) + " " + heading + " | " + vertical;

        if (HallowConfigManager.get().anchorPulse.showExactCoordinates) {
            return List.of(
                baseLine,
                String.format(Locale.ROOT, "Anchor XYZ: %.1f %.1f %.1f", anchor.x, anchor.y, anchor.z)
            );
        }

        return List.of(baseLine);
    }

    @Override
    public String legendLine(Minecraft client) {
        return slot() == 10
            ? "0  Anchor Pulse " + (anchor == null ? "[SET]" : "[CLEAR]")
            : super.legendLine(client);
    }

    @Override
    public void reset(Minecraft client) {
        anchor = null;
        anchorDimension = null;
    }

    public void loadAnchorState(HallowProfileState.SavedAnchor state) {
        HallowConfig config = HallowConfigManager.get();
        if (!config.anchorPulse.persistAnchorBetweenSessions || state == null || !state.present) {
            anchor = null;
            anchorDimension = null;
            return;
        }

        Identifier parsed = Identifier.tryParse(state.dimension);
        if (parsed == null) {
            anchor = null;
            anchorDimension = null;
            return;
        }

        anchor = new Vec3(state.x, state.y, state.z);
        anchorDimension = parsed;
    }

    public HallowProfileState.SavedAnchor exportAnchorState() {
        HallowProfileState.SavedAnchor state = new HallowProfileState.SavedAnchor();
        if (!HallowConfigManager.get().anchorPulse.persistAnchorBetweenSessions || anchor == null || anchorDimension == null) {
            return state;
        }

        state.present = true;
        state.dimension = anchorDimension.toString();
        state.x = anchor.x;
        state.y = anchor.y;
        state.z = anchor.z;
        return state;
    }
}
