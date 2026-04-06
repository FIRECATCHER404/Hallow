package com.hallow.client.cheat;

import java.util.List;

import com.hallow.client.config.HallowStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public abstract class CheatModule {
    private final String name;
    private final int slot;
    private final boolean toggleable;
    private boolean enabled;

    protected CheatModule(String name, int slot, boolean toggleable) {
        this.name = name;
        this.slot = slot;
        this.toggleable = toggleable;
    }

    public final void trigger(Minecraft client) {
        if (toggleable) {
            setEnabled(client, !enabled);
            announce(client, name + (enabled ? " enabled." : " disabled."));
        } else {
            activate(client);
        }
    }

    public void tick(Minecraft client) {
    }

    public List<String> hudLines(Minecraft client) {
        return enabled ? List.of(name + ": ON") : List.of();
    }

    public void reset(Minecraft client) {
        if (toggleable && enabled) {
            setEnabled(client, false);
        }
    }

    protected void activate(Minecraft client) {
    }

    protected void onEnable(Minecraft client) {
    }

    protected void onDisable(Minecraft client) {
    }

    protected final void announce(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[Hallow] " + message), true);
        }
    }

    public String legendLine(Minecraft client) {
        if (!toggleable) {
            return slotKeyLabel() + "  " + name;
        }

        return slotKeyLabel() + "  " + name + (enabled ? " [ON]" : " [OFF]");
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final String name() {
        return name;
    }

    public final int slot() {
        return slot;
    }

    public final boolean isToggleable() {
        return toggleable;
    }

    public final void setEnabled(Minecraft client, boolean enabled) {
        if (!toggleable || this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable(client);
        } else {
            onDisable(client);
        }
        HallowStorage.markDirty();
    }

    protected final String slotKeyLabel() {
        return switch (slot) {
            case 10 -> "0";
            case 11 -> "-";
            case 12 -> "=";
            case 13 -> "[";
            case 14 -> "]";
            case 15 -> "\\";
            default -> Integer.toString(slot);
        };
    }
}
