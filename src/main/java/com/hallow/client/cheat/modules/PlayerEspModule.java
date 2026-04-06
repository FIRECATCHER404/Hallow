package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class PlayerEspModule extends CheatModule {
    private static final double TRACK_RANGE = 96.0;

    public PlayerEspModule(int slot) {
        super("Player ESP", slot, true);
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        if (!isEnabled()) {
            return List.of();
        }

        return List.of("Player ESP: " + trackedPlayers(client).size() + " tracked");
    }

    public List<Player> trackedPlayers(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return List.of();
        }

        return client.level.players().stream()
            .filter(player -> player != client.player)
            .filter(Player::isAlive)
            .filter(player -> player.position().distanceToSqr(client.player.position()) <= (TRACK_RANGE * TRACK_RANGE))
            .map(player -> (Player) player)
            .toList();
    }
}
