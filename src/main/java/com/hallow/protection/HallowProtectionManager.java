package com.hallow.protection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public final class HallowProtectionManager {
    private static final Map<UUID, ProtectionState> STATES = new ConcurrentHashMap<>();

    private HallowProtectionManager() {
    }

    public static void update(ServerPlayer player, ProtectionState state) {
        if (player == null) {
            return;
        }

        STATES.put(player.getUUID(), state);
    }

    public static void remove(UUID playerUuid) {
        if (playerUuid != null) {
            STATES.remove(playerUuid);
        }
    }

    public static boolean blocksDamage(Player player, DamageSource source) {
        ProtectionState state = stateFor(player);
        if (state.invulnerable()) {
            return true;
        }

        if (source == null) {
            return false;
        }

        if (state.blockFireDamage() && source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }

        if (state.blockFallDamage() && source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }

        if (state.blockDrowningDamage() && source.is(DamageTypeTags.IS_DROWNING)) {
            return true;
        }

        if (state.blockFreezeDamage() && source.is(DamageTypeTags.IS_FREEZING)) {
            return true;
        }

        return state.blockPvpDamage() && isPvpDamage(source);
    }

    public static boolean blocksPvp(Player player) {
        ProtectionState state = stateFor(player);
        return state.invulnerable() || state.blockPvpDamage();
    }

    public static boolean blocksFallDamage(Player player) {
        ProtectionState state = stateFor(player);
        return state.invulnerable() || state.blockFallDamage();
    }

    public static boolean keepInventory(Player player) {
        return stateFor(player).keepInventory();
    }

    private static ProtectionState stateFor(Player player) {
        if (player == null) {
            return ProtectionState.DISABLED;
        }

        return STATES.getOrDefault(player.getUUID(), ProtectionState.DISABLED);
    }

    private static boolean isPvpDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_PLAYER_ATTACK) || source.getEntity() instanceof Player;
    }

    public record ProtectionState(
        boolean invulnerable,
        boolean blockDrowningDamage,
        boolean blockFallDamage,
        boolean blockFreezeDamage,
        boolean blockFireDamage,
        boolean keepInventory,
        boolean blockPvpDamage
    ) {
        public static final ProtectionState DISABLED = new ProtectionState(false, false, false, false, false, false, false);
    }
}
