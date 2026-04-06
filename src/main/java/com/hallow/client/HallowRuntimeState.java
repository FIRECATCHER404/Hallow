package com.hallow.client;

import java.util.UUID;

import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.XRayMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class HallowRuntimeState {
    private static volatile boolean xRayEnabled;
    private static volatile boolean creativeAccessEnabled;
    private static volatile UUID creativeAccessOwnerUuid;
    private static volatile boolean fullbrightEnabled;
    private static volatile boolean flightEnabled;
    private static volatile boolean stepAssistEnabled;
    private static volatile boolean safeWalkEnabled;
    private static volatile boolean noSlowEnabled;
    private static volatile boolean noPushEnabled;
    private static volatile boolean noWebEnabled;
    private static volatile boolean noRenderEnabled;

    private HallowRuntimeState() {
    }

    public static void setXRayEnabled(Minecraft client, boolean enabled) {
        if (xRayEnabled == enabled) {
            return;
        }

        xRayEnabled = enabled;
        requestLevelRerender(client);
    }

    public static boolean isXRayEnabled() {
        return xRayEnabled;
    }

    public static boolean isOreModeActive() {
        return xRayEnabled && HallowConfigManager.get().xray.mode == XRayMode.ORE_MODE;
    }

    public static boolean isSpectatorViewActive() {
        return xRayEnabled && HallowConfigManager.get().xray.mode == XRayMode.SPECTATOR_VIEW;
    }

    public static void setCreativeAccessEnabled(Minecraft client, boolean enabled) {
        creativeAccessEnabled = enabled;
        creativeAccessOwnerUuid = enabled && client != null && client.player != null
            ? client.player.getUUID()
            : null;
    }

    public static boolean isCreativeAccessEnabled() {
        return creativeAccessEnabled;
    }

    public static void setFullbrightEnabled(boolean enabled) {
        fullbrightEnabled = enabled;
    }

    public static boolean isFullbrightEnabled() {
        return fullbrightEnabled;
    }

    public static void setFlightEnabled(boolean enabled) {
        flightEnabled = enabled;
    }

    public static boolean isFlightEnabled() {
        return flightEnabled;
    }

    public static boolean hasCreativeAccess(Player player) {
        return creativeAccessEnabled
            && player != null
            && creativeAccessOwnerUuid != null
            && creativeAccessOwnerUuid.equals(player.getUUID());
    }

    public static void setStepAssistEnabled(boolean enabled) {
        stepAssistEnabled = enabled;
    }

    public static boolean isStepAssistEnabled() {
        return stepAssistEnabled;
    }

    public static float activeStepHeight() {
        return stepAssistEnabled ? (float) HallowConfigManager.get().stepAssist.height : 0.6F;
    }

    public static void setSafeWalkEnabled(boolean enabled) {
        safeWalkEnabled = enabled;
    }

    public static boolean isSafeWalkEnabled() {
        return safeWalkEnabled;
    }

    public static void setNoSlowEnabled(boolean enabled) {
        noSlowEnabled = enabled;
    }

    public static boolean isNoSlowEnabled() {
        return noSlowEnabled;
    }

    public static void setNoPushEnabled(boolean enabled) {
        noPushEnabled = enabled;
    }

    public static boolean isNoPushEnabled() {
        return noPushEnabled;
    }

    public static void setNoWebEnabled(boolean enabled) {
        noWebEnabled = enabled;
    }

    public static boolean isNoWebEnabled() {
        return noWebEnabled;
    }

    public static void setNoRenderEnabled(boolean enabled) {
        noRenderEnabled = enabled;
    }

    public static boolean isNoRenderEnabled() {
        return noRenderEnabled;
    }

    public static boolean shouldSpoofNoFall(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        boolean fallProtectionEnabled = HallowConfigManager.get().protection.invulnerable
            || HallowConfigManager.get().protection.blockFallDamage;
        if (!fallProtectionEnabled || player.isPassenger() || player.onGround()) {
            return false;
        }

        if (flightEnabled && player.getAbilities().flying) {
            return true;
        }

        return player.getDeltaMovement().y <= 0.0;
    }

    public static void onConfigSaved(Minecraft client) {
        XRayRules.reloadFromConfig();
        HallowClientNetworking.syncProtectionSettings(client);
        if (xRayEnabled) {
            requestLevelRerender(client);
        }
    }

    public static void requestLevelRerender(Minecraft client) {
        if (client != null && client.levelRenderer != null && client.level != null) {
            client.levelRenderer.allChanged();
        }
    }
}
