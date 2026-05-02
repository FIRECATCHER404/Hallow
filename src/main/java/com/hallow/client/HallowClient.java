package com.hallow.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.hallow.client.cheat.CheatModule;
import com.hallow.client.cheat.modules.AnchorPulseModule;
import com.hallow.client.cheat.modules.AutoToolModule;
import com.hallow.client.cheat.modules.AutoSprintModule;
import com.hallow.client.cheat.modules.BreadcrumbTrailModule;
import com.hallow.client.cheat.modules.ChestStealerModule;
import com.hallow.client.cheat.modules.CreativeAccessModule;
import com.hallow.client.cheat.modules.FlightModule;
import com.hallow.client.cheat.modules.FullbrightModule;
import com.hallow.client.cheat.modules.LootCompassModule;
import com.hallow.client.cheat.modules.NoPushModule;
import com.hallow.client.cheat.modules.NoRenderModule;
import com.hallow.client.cheat.modules.NoSlowModule;
import com.hallow.client.cheat.modules.NoWebModule;
import com.hallow.client.cheat.modules.PlayerEspModule;
import com.hallow.client.cheat.modules.ProjectilePredictModule;
import com.hallow.client.cheat.modules.SafeWalkModule;
import com.hallow.client.cheat.modules.StepAssistModule;
import com.hallow.client.cheat.modules.SwimAssistModule;
import com.hallow.client.cheat.modules.ThreatRadarModule;
import com.hallow.client.cheat.modules.XRayModule;
import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.HallowProfileState;
import com.hallow.client.config.HallowConfigScreen;
import com.hallow.client.config.HallowStorage;
import com.hallow.client.screen.HallowDeckScreen;
import com.hallow.network.HallowNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
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
    private static final int HUD_TOGGLE_KEY = GLFW.GLFW_KEY_F7;
    private static final int CREATIVE_SHORTCUT_KEY = GLFW.GLFW_KEY_V;
    private static final int MINIMAP_TOGGLE_KEY = GLFW.GLFW_KEY_COMMA;
    private static final int COPY_TARGET_MAINHAND_KEY = GLFW.GLFW_KEY_H;
    private static final int COPY_TARGET_OFFHAND_KEY = GLFW.GLFW_KEY_J;

    private final List<CheatModule> modules = new ArrayList<>();
    private final Set<Integer> shortcutPressedKeys = new HashSet<>();
    private final HallowStatusHudRenderer statusHudRenderer = new HallowStatusHudRenderer();

    private FullbrightModule fullbrightModule;
    private FlightModule flightModule;
    private XRayModule xRayModule;
    private ProjectilePredictModule projectilePredictModule;
    private LootCompassModule lootCompassModule;
    private ThreatRadarModule threatRadarModule;
    private BreadcrumbTrailModule breadcrumbTrailModule;
    private AutoSprintModule autoSprintModule;
    private StepAssistModule stepAssistModule;
    private SwimAssistModule swimAssistModule;
    private CreativeAccessModule creativeAccessModule;
    private AnchorPulseModule anchorPulseModule;
    private NoRenderModule noRenderModule;
    private SafeWalkModule safeWalkModule;
    private NoSlowModule noSlowModule;
    private NoPushModule noPushModule;
    private PlayerEspModule playerEspModule;
    private AutoToolModule autoToolModule;
    private ChestStealerModule chestStealerModule;
    private NoWebModule noWebModule;
    private HallowMinimapRenderer minimapRenderer;
    private boolean defaultsApplied;
    private boolean statusHudVisible;
    private boolean chordHeld;
    private boolean hudTogglePressed;
    private boolean creativeShortcutPressed;
    private boolean minimapTogglePressed;
    private boolean targetMainhandPressed;
    private boolean targetOffhandPressed;

    @Override
    public void onInitializeClient() {
        HallowConfigManager.load();
        XRayRules.reloadFromConfig();

        fullbrightModule = register(new FullbrightModule(GLFW.GLFW_KEY_1));
        flightModule = register(new FlightModule(GLFW.GLFW_KEY_2));
        xRayModule = register(new XRayModule(GLFW.GLFW_KEY_3));
        lootCompassModule = register(new LootCompassModule(GLFW.GLFW_KEY_4));
        threatRadarModule = register(new ThreatRadarModule(GLFW.GLFW_KEY_5));
        breadcrumbTrailModule = register(new BreadcrumbTrailModule(GLFW.GLFW_KEY_R));
        autoSprintModule = register(new AutoSprintModule(GLFW.GLFW_KEY_6));
        stepAssistModule = register(new StepAssistModule(GLFW.GLFW_KEY_7));
        swimAssistModule = register(new SwimAssistModule(GLFW.GLFW_KEY_8));
        creativeAccessModule = register(new CreativeAccessModule(GLFW.GLFW_KEY_9));
        anchorPulseModule = register(new AnchorPulseModule(GLFW.GLFW_KEY_0));
        projectilePredictModule = register(new ProjectilePredictModule(GLFW.GLFW_KEY_MINUS));
        noRenderModule = register(new NoRenderModule(GLFW.GLFW_KEY_EQUAL));
        safeWalkModule = register(new SafeWalkModule(GLFW.GLFW_KEY_LEFT_BRACKET));
        noSlowModule = register(new NoSlowModule(GLFW.GLFW_KEY_RIGHT_BRACKET));
        noPushModule = register(new NoPushModule(GLFW.GLFW_KEY_BACKSLASH));
        playerEspModule = register(new PlayerEspModule(GLFW.GLFW_KEY_P));
        autoToolModule = register(new AutoToolModule(GLFW.GLFW_KEY_T));
        chestStealerModule = register(new ChestStealerModule(GLFW.GLFW_KEY_C));
        noWebModule = register(new NoWebModule(GLFW.GLFW_KEY_W));
        minimapRenderer = new HallowMinimapRenderer();

        new XRayRenderer(xRayModule).register();
        new ProjectilePredictRenderer(projectilePredictModule).register();
        new PlayerEspRenderer(playerEspModule).register();
        new BreadcrumbTrailRenderer(breadcrumbTrailModule).register();

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

        handleChordInput(client);
        handleHudToggle(client);
        handleCreativeShortcut(client);
        handleMinimapToggle(client);
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

        if (HallowInputCapture.isChordCaptureActive(client)) {
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
            shortcutPressedKeys.clear();
            return;
        }

        long window = client.getWindow().handle();
        boolean captureActive = GLFW.glfwGetKey(window, CHORD_KEY) == GLFW.GLFW_PRESS;
        if (captureActive && !chordHeld) {
            KeyMapping.releaseAll();
            shortcutPressedKeys.clear();
        }

        chordHeld = captureActive;
        if (!chordHeld) {
            shortcutPressedKeys.clear();
            return;
        }

        for (CheatModule module : modules) {
            int shortcutKey = module.slot();
            boolean down = GLFW.glfwGetKey(window, shortcutKey) == GLFW.GLFW_PRESS;
            if (!down) {
                shortcutPressedKeys.remove(shortcutKey);
                continue;
            }

            if (shortcutPressedKeys.contains(shortcutKey)) {
                continue;
            }

            shortcutPressedKeys.add(shortcutKey);
            module.trigger(client);
        }
    }

    private void handleMinimapToggle(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            minimapTogglePressed = false;
            return;
        }

        if (HallowInputCapture.isChordCaptureActive(client)) {
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

    private void handleHudToggle(Minecraft client) {
        if (client.player == null || client.getWindow() == null) {
            hudTogglePressed = false;
            return;
        }

        if (client.screen != null && !(client.screen instanceof HallowDeckScreen)) {
            hudTogglePressed = false;
            return;
        }

        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), HUD_TOGGLE_KEY) == GLFW.GLFW_PRESS;
        if (!down) {
            hudTogglePressed = false;
            return;
        }

        if (hudTogglePressed) {
            return;
        }

        hudTogglePressed = true;
        if (client.screen instanceof HallowDeckScreen) {
            client.setScreen(null);
            return;
        }

        KeyMapping.releaseAll();
        shortcutPressedKeys.clear();
        creativeShortcutPressed = false;
        minimapTogglePressed = false;
        targetMainhandPressed = false;
        targetOffhandPressed = false;
        client.setScreen(new HallowDeckScreen(this));
    }

    private void handleTargetCopyShortcuts(Minecraft client) {
        if (client.player == null || client.getWindow() == null || client.screen != null) {
            targetMainhandPressed = false;
            targetOffhandPressed = false;
            return;
        }

        if (HallowInputCapture.isChordCaptureActive(client)) {
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
        if (client.screen instanceof HallowDeckScreen) {
            return;
        }

        renderStatusHud(graphics, client);
        renderSelectedPlayerOverlay(graphics, client);
        minimapRenderer.render(graphics, client);
    }

    public List<HallowHudRenderer.Section> buildDeckSections(Minecraft client) {
        List<HallowHudRenderer.Section> sections = new ArrayList<>();
        sections.add(new HallowHudRenderer.Section("Vision", "Render clarity, pack control, and visibility tools.", 0xFF7EA7DA, List.of(
            moduleEntry(client, fullbrightModule),
            moduleEntry(client, xRayModule),
            moduleEntry(client, noRenderModule),
            moduleEntry(client, playerEspModule)
        )));
        sections.add(new HallowHudRenderer.Section("Traversal", "Movement modules and moment-to-moment automation.", 0xFF61B887, List.of(
            moduleEntry(client, flightModule),
            moduleEntry(client, autoSprintModule),
            moduleEntry(client, stepAssistModule),
            moduleEntry(client, swimAssistModule),
            moduleEntry(client, autoToolModule),
            moduleEntry(client, safeWalkModule),
            moduleEntry(client, noSlowModule),
            moduleEntry(client, noPushModule),
            moduleEntry(client, noWebModule)
        )));
        sections.add(new HallowHudRenderer.Section("Awareness", "Nearby tracking, radar, and projectile previews.", 0xFF9A8BE6, List.of(
            moduleEntry(client, lootCompassModule),
            moduleEntry(client, threatRadarModule),
            moduleEntry(client, projectilePredictModule),
            moduleEntry(client, breadcrumbTrailModule)
        )));
        sections.add(new HallowHudRenderer.Section("Access", "Inventory tools, map toggles, and anchor utilities.", 0xFFE29F63, List.of(
            moduleEntry(client, creativeAccessModule),
            actionEntry("Open HallowInv", "Launch the inventory editor immediately.", "OPEN", () -> creativeAccessModule.openFromShortcut(client), true, 0xFFCB9344),
            moduleEntry(client, chestStealerModule),
            actionEntry("Anchor Pulse", anchorPulseDetail(client), anchorPulseBadge(client), () -> anchorPulseModule.trigger(client), true, 0xFFE2A654),
            actionEntry("Minimap", "Quick toggle for the terrain and player overlay.", onOff(minimapRenderer.isVisible()), () -> minimapRenderer.toggle(client), minimapRenderer.isVisible(), 0xFF66BFD7)
        )));
        sections.add(new HallowHudRenderer.Section("Camera", "Saved viewpoints, browse targets, lock, and follow tools.", 0xFF55B6C7, List.of(
            actionEntry("Save camera point", "Store the current view for this dimension. Total: " + HallowCameraController.savedPointCount(), "SAVE", () -> HallowCameraController.saveCurrentViewFromMenu(client), true, 0xFF55B6C7),
            actionEntry("Clear saved points here", "Remove stored camera points from the current dimension.", "CLEAR", () -> HallowCameraController.clearCurrentDimensionPointsFromMenu(client), HallowCameraController.savedPointCount() > 0, 0xFFC96C6C),
            actionEntry("Cycle saved views", "Jump through stored camera positions with N.", HallowCameraController.isFixedViewActive() ? "LIVE" : "BROWSE", () -> HallowCameraController.cycleSavedViewFromMenu(client), HallowCameraController.isFixedViewActive(), 0xFF7E95E8),
            actionEntry("Browse players", "Preview other players and use the mouse wheel to cycle.", onOff(HallowCameraController.isPlayerBrowseActive()), () -> HallowCameraController.togglePlayerBrowseFromMenu(client), HallowCameraController.isPlayerBrowseActive(), 0xFF55B6C7),
            actionEntry(lockTargetLabel(client), "Jump to the selected player camera and hold position.", HallowCameraController.isLockedTargetActive() ? "LOCKED" : "LOCK", () -> HallowCameraController.toggleLockCurrentTargetFromMenu(client), HallowCameraController.isLockedTargetActive(), 0xFF55B6C7),
            actionEntry(followTargetLabel(client), "Track the selected target until you return to your player.", HallowCameraController.isFollowTargetActive() ? "FOLLOW" : "TRACK", () -> HallowCameraController.toggleFollowCurrentTargetFromMenu(client), HallowCameraController.isFollowTargetActive(), 0xFF55B6C7)
        )));
        sections.add(new HallowHudRenderer.Section("Protection", "Damage shields and inventory safety for compatible sessions.", 0xFFC96C6C, List.of(
            protectionEntry(client, "Invulnerable", protection -> protection.invulnerable, (protection, value) -> protection.invulnerable = value),
            protectionEntry(client, "Block Fall Damage", protection -> protection.blockFallDamage, (protection, value) -> protection.blockFallDamage = value),
            protectionEntry(client, "Block Drowning", protection -> protection.blockDrowningDamage, (protection, value) -> protection.blockDrowningDamage = value),
            protectionEntry(client, "Block Freeze", protection -> protection.blockFreezeDamage, (protection, value) -> protection.blockFreezeDamage = value),
            protectionEntry(client, "Block Fire", protection -> protection.blockFireDamage, (protection, value) -> protection.blockFireDamage = value),
            protectionEntry(client, "Keep Inventory", protection -> protection.keepInventory, (protection, value) -> protection.keepInventory = value),
            protectionEntry(client, "Block PvP", protection -> protection.blockPvpDamage, (protection, value) -> protection.blockPvpDamage = value)
        )));
        sections.add(new HallowHudRenderer.Section("Utility", "Deck overlays, target copy helpers, and configuration access.", 0xFFB9C06E, List.of(
            actionEntry("Status HUD", "Toggle the lightweight in-game status overlay.", onOff(statusHudVisible), () -> toggleStatusHud(client), statusHudVisible, 0xFF5A8AC9),
            actionEntry("Copy selected mainhand", "Clone the selected live target's main hand into your current slot.", "COPY", () -> copySelectedTargetHand(client, false), HallowCameraController.currentLiveTarget(client) != null, 0xFFB9C06E),
            actionEntry("Copy selected offhand", "Clone the selected live target's offhand into your offhand slot.", "COPY", () -> copySelectedTargetHand(client, true), HallowCameraController.currentLiveTarget(client) != null, 0xFFB9C06E),
            actionEntry("Open config screen", "Edit saved defaults, ranges, and visual settings.", "OPEN", () -> client.setScreen(new HallowConfigScreen(client.screen)), true, 0xFF8FA0B8),
            actionEntry("Close menu", "Return to gameplay and keep current module states.", "ESC", () -> client.setScreen(null), true, 0xFF8FA0B8)
        )));
        return sections;
    }

    public int activeModuleCount() {
        int count = 0;
        for (CheatModule module : modules) {
            if (module.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public String currentProfileLabel() {
        HallowStorage.ProfileIdentity profile = HallowStorage.activeProfile();
        if (profile == null) {
            return "No active profile";
        }
        return profile.label() + " [" + profile.source() + "]";
    }

    private HallowHudRenderer.Entry moduleEntry(Minecraft client, CheatModule module) {
        boolean enabled = module.isEnabled();
        int accent = enabled ? 0xFF61B887 : 0xFF5B6A80;
        return actionEntry(
            module.name(),
            moduleDetail(client, module),
            enabled ? "ON" : "OFF",
            () -> module.trigger(client),
            enabled,
            accent
        );
    }

    private HallowHudRenderer.Entry protectionEntry(
        Minecraft client,
        String label,
        Function<HallowConfig.ProtectionSettings, Boolean> getter,
        BiConsumer<HallowConfig.ProtectionSettings, Boolean> setter
    ) {
        boolean enabled = getter.apply(HallowConfigManager.get().protection);
        return actionEntry(label, "Synced to singleplayer and Hallow-aware servers.", enabled ? "ON" : "OFF", () -> {
            HallowConfig next = HallowConfigManager.get().copy();
            boolean updated = !getter.apply(next.protection);
            setter.accept(next.protection, updated);
            HallowConfigManager.applyAndSave(next);
            HallowRuntimeState.onConfigSaved(client);
            announce(client, label + " " + (updated ? "enabled." : "disabled."));
        }, enabled, enabled ? 0xFF7EBE73 : 0xFF7B5C5C);
    }

    private HallowHudRenderer.Entry actionEntry(String label, String detail, String badge, Runnable action, boolean active, int accentColor) {
        return new HallowHudRenderer.Entry(label, detail, badge, action, accentColorForState(accentColor, active), active);
    }

    private HallowHudRenderer.Entry actionEntry(String label, String detail, Runnable action, boolean active, int accentColor) {
        return actionEntry(label, detail, "", action, active, accentColor);
    }

    private HallowHudRenderer.Entry actionEntry(String label, Runnable action, boolean active, int accentColor) {
        return actionEntry(label, "", "", action, active, accentColor);
    }

    private int accentColorForState(int accentColor, boolean active) {
        return active ? accentColor : dimAccent(accentColor);
    }

    private int dimAccent(int accentColor) {
        int red = (accentColor >>> 16) & 0xFF;
        int green = (accentColor >>> 8) & 0xFF;
        int blue = accentColor & 0xFF;
        red = Math.max(48, (int) (red * 0.6F));
        green = Math.max(48, (int) (green * 0.6F));
        blue = Math.max(48, (int) (blue * 0.6F));
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private String lockTargetLabel(Minecraft client) {
        String targetName = HallowCameraController.currentTargetName(client);
        if (HallowCameraController.isLockedTargetActive()) {
            return "Unlock target [" + fallbackName(targetName) + "]";
        }
        return "Lock current target [" + fallbackName(targetName) + "]";
    }

    private String followTargetLabel(Minecraft client) {
        String targetName = HallowCameraController.currentTargetName(client);
        if (HallowCameraController.isFollowTargetActive()) {
            return "Stop following [" + fallbackName(targetName) + "]";
        }
        return "Follow current target [" + fallbackName(targetName) + "]";
    }

    private String fallbackName(String name) {
        return name == null || name.isBlank() ? "none" : name;
    }

    private String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private String moduleDetail(Minecraft client, CheatModule module) {
        if (module.isEnabled()) {
            List<String> lines = module.hudLines(client);
            if (!lines.isEmpty()) {
                return lines.getFirst();
            }
        }
        return "Shortcut: hold F6 then press " + module.shortcutLabel() + ".";
    }

    private String anchorPulseBadge(Minecraft client) {
        return anchorPulseModule.legendLine(client).contains("[CLEAR]") ? "CLEAR" : "SET";
    }

    private String anchorPulseDetail(Minecraft client) {
        List<String> lines = anchorPulseModule.hudLines(client);
        if (!lines.isEmpty()) {
            return lines.getFirst();
        }
        return "Store the current position as a tracked anchor.";
    }

    public boolean isStatusHudVisible() {
        return statusHudVisible;
    }

    public void toggleStatusHud(Minecraft client) {
        setStatusHudVisible(!statusHudVisible, true);
        announce(client, statusHudVisible ? "Status HUD enabled." : "Status HUD hidden.");
    }

    private void resetModules(Minecraft client) {
        defaultsApplied = false;
        statusHudVisible = false;
        chordHeld = false;
        hudTogglePressed = false;
        creativeShortcutPressed = false;
        minimapTogglePressed = false;
        targetMainhandPressed = false;
        targetOffhandPressed = false;
        shortcutPressedKeys.clear();
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
        HallowRuntimeState.setNoWebEnabled(false);
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
        applyModuleState(client, breadcrumbTrailModule, state, config.breadcrumbTrail.autoEnable);
        applyModuleState(client, autoSprintModule, state, config.autoSprint.autoEnable);
        applyModuleState(client, stepAssistModule, state, config.stepAssist.autoEnable);
        applyModuleState(client, swimAssistModule, state, config.swimAssist.autoEnable);
        applyModuleState(client, safeWalkModule, state, config.safeWalk.autoEnable);
        applyModuleState(client, noSlowModule, state, config.noSlow.autoEnable);
        applyModuleState(client, noPushModule, state, config.noPush.autoEnable);
        applyModuleState(client, playerEspModule, state, config.playerEsp.autoEnable);
        applyModuleState(client, autoToolModule, state, config.autoTool.autoEnable);
        applyModuleState(client, chestStealerModule, state, config.chestStealer.autoEnable);
        applyModuleState(client, noWebModule, state, config.noWeb.autoEnable);
        creativeAccessModule.restoreEnabledState(client, moduleEnabled(state, creativeAccessModule.name(), config.creativeAccess.autoEnable));
        applyModuleState(client, noRenderModule, state, config.noRender.autoEnable);

        setStatusHudVisible(state.loadedFromDisk ? state.hudVisible : config.hud.startVisible, false);
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
        state.hudVisible = statusHudVisible;
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

    private void renderStatusHud(GuiGraphics graphics, Minecraft client) {
        if (!statusHudVisible || client.player == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("Profile: " + currentProfileLabel());
        lines.add("Modules: " + activeModuleCount() + " active | Minimap " + onOff(minimapRenderer.isVisible()));
        for (CheatModule module : modules) {
            lines.addAll(module.hudLines(client));
        }
        lines.addAll(HallowCameraController.hudLines(client));

        HallowConfig.HudSettings hudConfig = HallowConfigManager.get().hud;
        statusHudRenderer.render(graphics, client, "Hallow HUD", lines, hudConfig.left, hudConfig.top);
    }

    private void renderSelectedPlayerOverlay(GuiGraphics graphics, Minecraft client) {
        HallowCameraController.TargetHudState target = HallowCameraController.currentTargetHudState(client);
        if (target == null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int centerX = screenWidth / 2;
        int top = 8;
        String title = "Selected: " + target.name();
        String detail = target.livePlayer() == null
            ? "Snapshot only: H/J unavailable"
            : "H main -> held | J off -> offhand";
        int textWidth = Math.max(client.font.width(title), client.font.width(detail));
        int panelWidth = Math.min(screenWidth - 16, Math.max(180, textWidth + 18));
        int left = Math.max(8, centerX - panelWidth / 2);
        int right = Math.min(screenWidth - 8, left + panelWidth);
        centerX = left + ((right - left) / 2);
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
        return module;
    }

    private void setStatusHudVisible(boolean visible, boolean markDirty) {
        if (statusHudVisible == visible) {
            return;
        }

        statusHudVisible = visible;
        if (markDirty) {
            HallowStorage.markDirty();
        }
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
