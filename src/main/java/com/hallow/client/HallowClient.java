package com.hallow.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.hallow.client.cheat.CheatModule;
import com.hallow.client.cheat.modules.AnchorPulseModule;
import com.hallow.client.cheat.modules.AutoSprintModule;
import com.hallow.client.cheat.modules.CreativeAccessModule;
import com.hallow.client.cheat.modules.FlightModule;
import com.hallow.client.cheat.modules.FullbrightModule;
import com.hallow.client.cheat.modules.LootCompassModule;
import com.hallow.client.cheat.modules.NoPushModule;
import com.hallow.client.cheat.modules.NoRenderModule;
import com.hallow.client.cheat.modules.NoSlowModule;
import com.hallow.client.cheat.modules.ProjectilePredictModule;
import com.hallow.client.cheat.modules.SafeWalkModule;
import com.hallow.client.cheat.modules.StepAssistModule;
import com.hallow.client.cheat.modules.SwimAssistModule;
import com.hallow.client.cheat.modules.ThreatRadarModule;
import com.hallow.client.cheat.modules.XRayModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.HallowProfileState;
import com.hallow.client.config.HallowStorage;
import com.hallow.network.HallowNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HallowClient implements ClientModInitializer {
    public static final String MOD_ID = "hallow";
    private static final int CHORD_KEY = GLFW.GLFW_KEY_F6;
    private static final int CREATIVE_SHORTCUT_KEY = GLFW.GLFW_KEY_V;
    private static final int MINIMAP_TOGGLE_KEY = GLFW.GLFW_KEY_COMMA;
    private static final int COPY_TARGET_MAINHAND_KEY = GLFW.GLFW_KEY_H;
    private static final int COPY_TARGET_OFFHAND_KEY = GLFW.GLFW_KEY_J;
    private static final int[] SLOT_KEYS = {
        GLFW.GLFW_KEY_1,
        GLFW.GLFW_KEY_2,
        GLFW.GLFW_KEY_3,
        GLFW.GLFW_KEY_4,
        GLFW.GLFW_KEY_5,
        GLFW.GLFW_KEY_6,
        GLFW.GLFW_KEY_7,
        GLFW.GLFW_KEY_8,
        GLFW.GLFW_KEY_9,
        GLFW.GLFW_KEY_0,
        GLFW.GLFW_KEY_MINUS,
        GLFW.GLFW_KEY_EQUAL,
        GLFW.GLFW_KEY_LEFT_BRACKET,
        GLFW.GLFW_KEY_RIGHT_BRACKET,
        GLFW.GLFW_KEY_BACKSLASH
    };

    private final List<CheatModule> modules = new ArrayList<>();
    private final CheatModule[] slots = new CheatModule[SLOT_KEYS.length];
    private final boolean[] slotPressed = new boolean[SLOT_KEYS.length];

    private FullbrightModule fullbrightModule;
    private FlightModule flightModule;
    private XRayModule xRayModule;
    private ProjectilePredictModule projectilePredictModule;
    private LootCompassModule lootCompassModule;
    private ThreatRadarModule threatRadarModule;
    private AutoSprintModule autoSprintModule;
    private StepAssistModule stepAssistModule;
    private SwimAssistModule swimAssistModule;
    private CreativeAccessModule creativeAccessModule;
    private AnchorPulseModule anchorPulseModule;
    private NoRenderModule noRenderModule;
    private SafeWalkModule safeWalkModule;
    private NoSlowModule noSlowModule;
    private NoPushModule noPushModule;
    private HallowMinimapRenderer minimapRenderer;
    private boolean defaultsApplied;
    private boolean chordHeld;
    private boolean creativeShortcutPressed;
    private boolean minimapTogglePressed;
    private boolean targetMainhandPressed;
    private boolean targetOffhandPressed;

    @Override
    public void onInitializeClient() {
        HallowConfigManager.load();
        XRayRules.reloadFromConfig();

        fullbrightModule = register(new FullbrightModule(1));
        flightModule = register(new FlightModule(2));
        xRayModule = register(new XRayModule(3));
        lootCompassModule = register(new LootCompassModule(4));
        threatRadarModule = register(new ThreatRadarModule(5));
        autoSprintModule = register(new AutoSprintModule(6));
        stepAssistModule = register(new StepAssistModule(7));
        swimAssistModule = register(new SwimAssistModule(8));
        creativeAccessModule = register(new CreativeAccessModule(9));
        anchorPulseModule = register(new AnchorPulseModule(10));
        projectilePredictModule = register(new ProjectilePredictModule(11));
        noRenderModule = register(new NoRenderModule(12));
        safeWalkModule = register(new SafeWalkModule(13));
        noSlowModule = register(new NoSlowModule(14));
        noPushModule = register(new NoPushModule(15));
        minimapRenderer = new HallowMinimapRenderer();

        new XRayRenderer(xRayModule).register();
        new ProjectilePredictRenderer(projectilePredictModule).register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            defaultsApplied = false;
            HallowClientNetworking.syncProtectionSettings(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            saveRuntimeState(client);
            resetModules(client);
            HallowStorage.clearActiveProfile();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            saveRuntimeState(client);
            resetModules(client);
            HallowStorage.clearActiveProfile();
        });
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
            this::renderHud
        );
    }

    private void onClientTick(Minecraft client) {
        if (client.player != null && !defaultsApplied) {
            applySessionState(client);
            defaultsApplied = true;
        } else if (client.player == null) {
            defaultsApplied = false;
        }

        handleCreativeShortcut(client);
        handleMinimapToggle(client);
        handleChordInput(client);
        HallowCameraController.tick(client);
        handleTargetCopyShortcuts(client);

        for (CheatModule module : modules) {
            module.tick(client);
        }

        if (client.player != null && defaultsApplied && HallowStorage.isDirty()) {
            saveRuntimeState(client);
        }
    }

    private void handleCreativeShortcut(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            creativeShortcutPressed = false;
            return;
        }

        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), CREATIVE_SHORTCUT_KEY) == GLFW.GLFW_PRESS;
        if (!down) {
            creativeShortcutPressed = false;
            return;
        }

        if (creativeShortcutPressed) {
            return;
        }

        creativeShortcutPressed = true;
        creativeAccessModule.openFromShortcut(client);
    }

    private void handleChordInput(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            chordHeld = false;
            Arrays.fill(slotPressed, false);
            return;
        }

        long window = client.getWindow().handle();
        chordHeld = GLFW.glfwGetKey(window, CHORD_KEY) == GLFW.GLFW_PRESS;
        if (!chordHeld) {
            Arrays.fill(slotPressed, false);
            return;
        }

        for (int index = 0; index < SLOT_KEYS.length; index++) {
            boolean down = GLFW.glfwGetKey(window, SLOT_KEYS[index]) == GLFW.GLFW_PRESS;
            if (!down) {
                slotPressed[index] = false;
                continue;
            }

            if (slotPressed[index]) {
                continue;
            }

            slotPressed[index] = true;
            CheatModule module = slots[index];
            if (module != null) {
                module.trigger(client);
            }
        }
    }

    private void handleMinimapToggle(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            minimapTogglePressed = false;
            return;
        }

        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), MINIMAP_TOGGLE_KEY) == GLFW.GLFW_PRESS;
        if (!down) {
            minimapTogglePressed = false;
            return;
        }

        if (minimapTogglePressed) {
            return;
        }

        minimapTogglePressed = true;
        minimapRenderer.toggle(client);
    }

    private void handleTargetCopyShortcuts(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            targetMainhandPressed = false;
            targetOffhandPressed = false;
            return;
        }

        long window = client.getWindow().handle();
        boolean mainhandDown = GLFW.glfwGetKey(window, COPY_TARGET_MAINHAND_KEY) == GLFW.GLFW_PRESS;
        if (mainhandDown && !targetMainhandPressed) {
            copySelectedTargetHand(client, false);
        }
        targetMainhandPressed = mainhandDown;

        boolean offhandDown = GLFW.glfwGetKey(window, COPY_TARGET_OFFHAND_KEY) == GLFW.GLFW_PRESS;
        if (offhandDown && !targetOffhandPressed) {
            copySelectedTargetHand(client, true);
        }
        targetOffhandPressed = offhandDown;
    }

    private void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        List<String> lines = new ArrayList<>();

        for (CheatModule module : modules) {
            lines.addAll(module.hudLines(client));
        }
        lines.addAll(HallowCameraController.hudLines(client));

        if (chordHeld) {
            if (!lines.isEmpty()) {
                lines.add("");
            }

            for (CheatModule module : modules) {
                lines.add(module.legendLine(client));
            }
        }

        if (!lines.isEmpty()) {
            String title = chordHeld ? "Hallow // F6 held" : "Hallow";
            int widestLine = client.font.width(title);
            for (String line : lines) {
                widestLine = Math.max(widestLine, client.font.width(line));
            }

            HallowConfig config = HallowConfigManager.get();
            int left = config.hud.left;
            int top = config.hud.top;
            int lineHeight = client.font.lineHeight + 2;
            int height = (lines.size() + 1) * lineHeight + 8;
            int width = widestLine + 12;

            graphics.fill(left - 6, top - 6, left + width + 2, top + height + 2, 0x88070A10);
            graphics.fill(left - 4, top - 4, left + width, top + height, 0xC0121823);
            graphics.fill(left - 4, top - 4, left + width, top - 2, 0xFFB88C3A);
            graphics.drawString(client.font, title, left, top, 0xFFF2D8A0, true);

            int y = top + lineHeight;
            for (String line : lines) {
                if (!line.isEmpty()) {
                    graphics.drawString(client.font, line, left, y, 0xFFFFFFFF, true);
                }
                y += lineHeight;
            }
        }

        renderSelectedPlayerOverlay(graphics, client);
        minimapRenderer.render(graphics, client);
    }

    private void resetModules(Minecraft client) {
        defaultsApplied = false;
        chordHeld = false;
        creativeShortcutPressed = false;
        minimapTogglePressed = false;
        targetMainhandPressed = false;
        targetOffhandPressed = false;
        Arrays.fill(slotPressed, false);
        for (CheatModule module : modules) {
            module.reset(client);
        }
        HallowCameraController.reset(client);
        minimapRenderer.reset();
        HallowRuntimeState.setCreativeAccessEnabled(client, false);
        HallowRuntimeState.setStepAssistEnabled(false);
        HallowRuntimeState.setSafeWalkEnabled(false);
        HallowRuntimeState.setNoSlowEnabled(false);
        HallowRuntimeState.setNoPushEnabled(false);
        HallowRuntimeState.setNoRenderEnabled(false);
    }

    private void applySessionState(Minecraft client) {
        HallowStorage.ProfileIdentity profile = HallowStorage.resolveProfileIdentity(client);
        HallowStorage.activateProfile(profile);

        HallowConfig config = HallowConfigManager.get();
        HallowProfileState state = HallowStorage.loadActiveProfile();

        applyModuleState(client, fullbrightModule, state, config.fullbright.autoEnable);
        applyModuleState(client, flightModule, state, config.fly.autoEnable);
        applyModuleState(client, xRayModule, state, config.xray.autoEnable);
        applyModuleState(client, projectilePredictModule, state, config.projectilePredict.autoEnable);
        applyModuleState(client, lootCompassModule, state, config.lootCompass.autoEnable);
        applyModuleState(client, threatRadarModule, state, config.threatRadar.autoEnable);
        applyModuleState(client, autoSprintModule, state, config.autoSprint.autoEnable);
        applyModuleState(client, stepAssistModule, state, config.stepAssist.autoEnable);
        applyModuleState(client, swimAssistModule, state, config.swimAssist.autoEnable);
        applyModuleState(client, safeWalkModule, state, config.safeWalk.autoEnable);
        applyModuleState(client, noSlowModule, state, config.noSlow.autoEnable);
        applyModuleState(client, noPushModule, state, config.noPush.autoEnable);
        creativeAccessModule.restoreEnabledState(client, moduleEnabled(state, creativeAccessModule.name(), config.creativeAccess.autoEnable));
        applyModuleState(client, noRenderModule, state, config.noRender.autoEnable);

        minimapRenderer.setVisible(state.loadedFromDisk ? state.minimapVisible : config.minimap.startEnabled);
        HallowCameraController.loadSavedPoints(state.savedCameras.stream()
            .map(camera -> new HallowCameraController.CameraPoint(camera.dimension, new net.minecraft.world.phys.Vec3(camera.x, camera.y, camera.z), camera.yaw, camera.pitch))
            .toList());
        anchorPulseModule.loadAnchorState(state.anchor);
        HallowStorage.markDirty();
    }

    private void applyModuleState(Minecraft client, CheatModule module, HallowProfileState state, boolean fallback) {
        module.setEnabled(client, moduleEnabled(state, module.name(), fallback));
    }

    private boolean moduleEnabled(HallowProfileState state, String moduleName, boolean fallback) {
        if (state.enabledModules.containsKey(moduleName)) {
            return Boolean.TRUE.equals(state.enabledModules.get(moduleName));
        }
        return fallback;
    }

    private void saveRuntimeState(Minecraft client) {
        HallowStorage.ProfileIdentity profile = HallowStorage.activeProfile();
        if (client == null || profile == null) {
            return;
        }

        HallowProfileState state = new HallowProfileState();
        for (CheatModule module : modules) {
            if (module.isToggleable()) {
                state.enabledModules.put(module.name(), module.isEnabled());
            }
        }
        state.minimapVisible = minimapRenderer.isVisible();
        state.savedCameras = HallowCameraController.exportSavedPoints().stream().map(point -> {
            HallowProfileState.SavedCamera saved = new HallowProfileState.SavedCamera();
            saved.dimension = point.dimension();
            saved.x = point.position().x;
            saved.y = point.position().y;
            saved.z = point.position().z;
            saved.yaw = point.yaw();
            saved.pitch = point.pitch();
            return saved;
        }).toList();
        state.anchor = anchorPulseModule.exportAnchorState();
        HallowStorage.saveActiveProfile(state);
    }

    private void renderSelectedPlayerOverlay(GuiGraphics graphics, Minecraft client) {
        HallowCameraController.TargetHudState target = HallowCameraController.currentTargetHudState(client);
        if (target == null) {
            return;
        }

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int top = 8;
        String title = "Selected: " + target.name();
        String detail = target.livePlayer() == null
            ? "Snapshot only: H/J unavailable"
            : "H main -> held | J off -> offhand";
        int textWidth = Math.max(client.font.width(title), client.font.width(detail));
        int panelWidth = Math.max(180, textWidth + 18);
        int left = centerX - panelWidth / 2;
        int right = left + panelWidth;
        int bottom = top + 54;

        graphics.fill(left, top, right, bottom, 0xCC11161E);
        graphics.fill(left, top, right, top + 2, 0xFFB88C3A);
        graphics.drawCenteredString(client.font, title, centerX, top + 6, 0xFFF7E4BC);

        if (target.livePlayer() == null) {
            graphics.drawCenteredString(client.font, "Snapshot view only", centerX, top + 22, 0xFFE0C689);
            graphics.drawCenteredString(client.font, detail, centerX, top + 34, 0xFFB9C0CC);
            return;
        }

        renderTargetLoadout(graphics, client, target.livePlayer(), left + 12, top + 20);
        graphics.drawCenteredString(client.font, detail, centerX, top + 42, 0xFFB9C0CC);
    }

    private void renderTargetLoadout(GuiGraphics graphics, Minecraft client, Player player, int left, int top) {
        ItemStack[] stacks = new ItemStack[] {
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET),
            player.getMainHandItem(),
            player.getOffhandItem()
        };
        String[] labels = new String[] { "H", "C", "L", "F", "M", "O" };

        for (int index = 0; index < stacks.length; index++) {
            int x = left + index * 18;
            graphics.fill(x - 1, top - 1, x + 17, top + 17, 0xFF303B48);
            if (!stacks[index].isEmpty()) {
                graphics.renderItem(stacks[index], x, top);
                graphics.renderItemDecorations(client.font, stacks[index], x, top);
            }
            graphics.drawCenteredString(client.font, labels[index], x + 8, top + 19, 0xFFE0C689);
        }
    }

    private <T extends CheatModule> T register(T module) {
        modules.add(module);
        slots[module.slot() - 1] = module;
        return module;
    }

    private void copySelectedTargetHand(Minecraft client, boolean offhand) {
        if (client.player == null) {
            return;
        }

        HallowCameraController.TargetHudState targetHud = HallowCameraController.currentTargetHudState(client);
        Player target = HallowCameraController.currentLiveTarget(client);
        if (target == null) {
            if (targetHud != null) {
                announce(client, "Live target required for H/J hand copy.");
            }
            return;
        }

        ItemStack copied = offhand ? target.getOffhandItem() : target.getMainHandItem();
        int inventorySlot = offhand ? HallowNetworking.OFFHAND_SLOT : client.player.getInventory().getSelectedSlot();
        creativeAccessModule.assignStackToInventory(client, copied.copy(), inventorySlot);

        String slotLabel = offhand ? "offhand" : "held slot";
        String sourceLabel = offhand ? "offhand" : "main hand";
        if (copied.isEmpty()) {
            announce(client, "Cleared your " + slotLabel + " from " + target.getGameProfile().name() + "'s " + sourceLabel + ".");
            return;
        }

        announce(client, "Copied " + target.getGameProfile().name() + "'s " + sourceLabel + " to your " + slotLabel + ".");
    }

    private void announce(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[Hallow] " + message), true);
        }
    }
}
