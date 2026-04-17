package com.hallow.client.cheat;

import java.util.List;
import java.util.Locale;

import com.hallow.client.config.HallowStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

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

    public final String shortcutLabel() {
        return slotKeyLabel();
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
            case GLFW.GLFW_KEY_0 -> "0";
            case GLFW.GLFW_KEY_1 -> "1";
            case GLFW.GLFW_KEY_2 -> "2";
            case GLFW.GLFW_KEY_3 -> "3";
            case GLFW.GLFW_KEY_4 -> "4";
            case GLFW.GLFW_KEY_5 -> "5";
            case GLFW.GLFW_KEY_6 -> "6";
            case GLFW.GLFW_KEY_7 -> "7";
            case GLFW.GLFW_KEY_8 -> "8";
            case GLFW.GLFW_KEY_9 -> "9";
            case GLFW.GLFW_KEY_A -> "A";
            case GLFW.GLFW_KEY_B -> "B";
            case GLFW.GLFW_KEY_C -> "C";
            case GLFW.GLFW_KEY_D -> "D";
            case GLFW.GLFW_KEY_E -> "E";
            case GLFW.GLFW_KEY_F -> "F";
            case GLFW.GLFW_KEY_G -> "G";
            case GLFW.GLFW_KEY_H -> "H";
            case GLFW.GLFW_KEY_I -> "I";
            case GLFW.GLFW_KEY_J -> "J";
            case GLFW.GLFW_KEY_K -> "K";
            case GLFW.GLFW_KEY_L -> "L";
            case GLFW.GLFW_KEY_M -> "M";
            case GLFW.GLFW_KEY_N -> "N";
            case GLFW.GLFW_KEY_O -> "O";
            case GLFW.GLFW_KEY_P -> "P";
            case GLFW.GLFW_KEY_Q -> "Q";
            case GLFW.GLFW_KEY_R -> "R";
            case GLFW.GLFW_KEY_S -> "S";
            case GLFW.GLFW_KEY_T -> "T";
            case GLFW.GLFW_KEY_U -> "U";
            case GLFW.GLFW_KEY_V -> "V";
            case GLFW.GLFW_KEY_W -> "W";
            case GLFW.GLFW_KEY_X -> "X";
            case GLFW.GLFW_KEY_Y -> "Y";
            case GLFW.GLFW_KEY_Z -> "Z";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_COMMA -> ",";
            default -> {
                String keyName = GLFW.glfwGetKeyName(slot, 0);
                yield keyName == null ? Integer.toString(slot) : keyName.toUpperCase(Locale.ROOT);
            }
        };
    }
}
