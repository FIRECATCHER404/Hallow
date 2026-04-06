package com.hallow.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;

public final class HallowInputCapture {
    private HallowInputCapture() {
    }

    public static boolean isChordCaptureActive() {
        return isChordCaptureActive(Minecraft.getInstance());
    }

    public static boolean isChordCaptureActive(Minecraft client) {
        if (client == null || client.player == null || client.getWindow() == null || client.screen != null) {
            return false;
        }

        return GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_F6) == GLFW.GLFW_PRESS;
    }
}
