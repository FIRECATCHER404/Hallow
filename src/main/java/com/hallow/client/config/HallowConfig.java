package com.hallow.client.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import net.minecraft.resources.Identifier;

public final class HallowConfig {
    private static final List<String> DEFAULT_XRAY_BLOCKS = List.of(
        "minecraft:coal_ore",
        "minecraft:deepslate_coal_ore",
        "minecraft:copper_ore",
        "minecraft:deepslate_copper_ore",
        "minecraft:diamond_ore",
        "minecraft:deepslate_diamond_ore",
        "minecraft:emerald_ore",
        "minecraft:deepslate_emerald_ore",
        "minecraft:redstone_ore",
        "minecraft:deepslate_redstone_ore",
        "minecraft:lapis_ore",
        "minecraft:deepslate_lapis_ore",
        "minecraft:gold_ore",
        "minecraft:deepslate_gold_ore",
        "minecraft:iron_ore",
        "minecraft:deepslate_iron_ore",
        "minecraft:nether_gold_ore",
        "minecraft:nether_quartz_ore",
        "minecraft:ancient_debris"
    );

    public HudSettings hud = new HudSettings();
    public FullbrightSettings fullbright = new FullbrightSettings();
    public FlySettings fly = new FlySettings();
    public XRaySettings xray = new XRaySettings();
    public ProjectilePredictSettings projectilePredict = new ProjectilePredictSettings();
    public NoRenderSettings noRender = new NoRenderSettings();
    public LootCompassSettings lootCompass = new LootCompassSettings();
    public ThreatRadarSettings threatRadar = new ThreatRadarSettings();
    public AutoSprintSettings autoSprint = new AutoSprintSettings();
    public StepAssistSettings stepAssist = new StepAssistSettings();
    public SwimAssistSettings swimAssist = new SwimAssistSettings();
    public PlayerEspSettings playerEsp = new PlayerEspSettings();
    public AutoToolSettings autoTool = new AutoToolSettings();
    public ChestStealerSettings chestStealer = new ChestStealerSettings();
    public SafeWalkSettings safeWalk = new SafeWalkSettings();
    public NoSlowSettings noSlow = new NoSlowSettings();
    public NoPushSettings noPush = new NoPushSettings();
    public NoWebSettings noWeb = new NoWebSettings();
    public CreativeAccessSettings creativeAccess = new CreativeAccessSettings();
    public CameraSettings camera = new CameraSettings();
    public MinimapSettings minimap = new MinimapSettings();
    public ProtectionSettings protection = new ProtectionSettings();
    public AnchorPulseSettings anchorPulse = new AnchorPulseSettings();
    public PersistedAnchor persistedAnchor = new PersistedAnchor();

    public HallowConfig copy() {
        return HallowConfigManager.GSON.fromJson(HallowConfigManager.GSON.toJson(this), HallowConfig.class);
    }

    public void normalize() {
        hud.left = clamp(hud.left, 0, 200);
        hud.top = clamp(hud.top, 0, 200);

        fullbright.gamma = clamp(fullbright.gamma, 0.0, 1.0);

        fly.speed = clamp(fly.speed, 0.05, 0.5);

        xray.horizontalRadius = clamp(xray.horizontalRadius, 4, 24);
        xray.verticalRadius = clamp(xray.verticalRadius, 4, 16);
        xray.maxTargets = clamp(xray.maxTargets, 16, 256);
        xray.scanInterval = clamp(xray.scanInterval, 4, 40);
        xray.spectatorPeekDistance = clamp(xray.spectatorPeekDistance, 1.0, 8.0);
        xray.spectatorPush = clamp(xray.spectatorPush, 0.05, 1.5);
        xray.trackedBlocks = normalizeBlockIds(xray.trackedBlocks, DEFAULT_XRAY_BLOCKS);

        projectilePredict.maxSteps = clamp(projectilePredict.maxSteps, 30, 240);
        projectilePredict.lineWidth = clamp(projectilePredict.lineWidth, 1.0, 6.0);
        projectilePredict.arcRed = clamp(projectilePredict.arcRed, 0.0, 1.0);
        projectilePredict.arcGreen = clamp(projectilePredict.arcGreen, 0.0, 1.0);
        projectilePredict.arcBlue = clamp(projectilePredict.arcBlue, 0.0, 1.0);

        camera.maxSavedPoints = clamp(camera.maxSavedPoints, 1, 64);
        minimap.size = clamp(minimap.size, 96, 196);
        minimap.range = clamp(minimap.range, 32.0, 192.0);

        lootCompass.range = clamp(lootCompass.range, 8.0, 96.0);
        lootCompass.scanInterval = clamp(lootCompass.scanInterval, 2, 40);

        threatRadar.range = clamp(threatRadar.range, 8.0, 96.0);
        threatRadar.scanInterval = clamp(threatRadar.scanInterval, 2, 40);
        threatRadar.blindsideThreshold = clamp(threatRadar.blindsideThreshold, -0.95, 0.95);

        stepAssist.height = clamp(stepAssist.height, 0.6, 2.0);

        swimAssist.horizontalBoost = clamp(swimAssist.horizontalBoost, 0.01, 0.2);
        swimAssist.verticalBoost = clamp(swimAssist.verticalBoost, 0.01, 0.15);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static List<String> defaultXRayBlockIds() {
        return new ArrayList<>(DEFAULT_XRAY_BLOCKS);
    }

    public static List<String> normalizeBlockIds(List<String> values) {
        return normalizeBlockIds(values, DEFAULT_XRAY_BLOCKS);
    }

    public static String normalizeBlockId(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }

        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        Identifier identifier = Identifier.tryParse(normalized);
        return identifier != null ? identifier.toString() : "";
    }

    private static List<String> normalizeBlockIds(List<String> values, List<String> defaults) {
        List<String> source = values != null ? values : defaults;
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();

        for (String value : source) {
            String normalized = normalizeBlockId(value);
            if (!normalized.isEmpty()) {
                normalizedIds.add(normalized);
            }
        }

        if (normalizedIds.isEmpty()) {
            normalizedIds.addAll(defaults);
        }

        return new ArrayList<>(normalizedIds);
    }

    public static final class HudSettings {
        public boolean startVisible = true;
        public int left = 8;
        public int top = 8;
    }

    public static final class FullbrightSettings {
        public boolean autoEnable = false;
        public double gamma = 1.0;
        public boolean reduceDarkness = true;
    }

    public static final class FlySettings {
        public boolean autoEnable = false;
        public double speed = 0.1;
    }

    public static final class XRaySettings {
        public boolean autoEnable = false;
        public XRayMode mode = XRayMode.ESP;
        public int horizontalRadius = 12;
        public int verticalRadius = 8;
        public int maxTargets = 96;
        public int scanInterval = 12;
        public double spectatorPeekDistance = 4.5;
        public double spectatorPush = 0.35;
        public List<String> trackedBlocks = defaultXRayBlockIds();
    }

    public static final class ProjectilePredictSettings {
        public boolean autoEnable = false;
        public int maxSteps = 120;
        public double lineWidth = 2.0;
        public boolean showLandingMarker = true;
        public double arcRed = 0.35;
        public double arcGreen = 0.95;
        public double arcBlue = 0.70;
    }

    public static final class NoRenderSettings {
        public boolean autoEnable = false;
        public boolean hideFog = true;
        public boolean hideCameraOverlays = true;
        public boolean hideViewBobbing = true;
        public boolean hideDamageTilt = true;
    }

    public static final class LootCompassSettings {
        public boolean autoEnable = false;
        public double range = 24.0;
        public int scanInterval = 8;
    }

    public static final class ThreatRadarSettings {
        public boolean autoEnable = false;
        public double range = 28.0;
        public int scanInterval = 8;
        public double blindsideThreshold = -0.15;
        public boolean highlightPlayers = false;
    }

    public static final class AutoSprintSettings {
        public boolean autoEnable = false;
        public boolean keepWhileUsingItem = false;
    }

    public static final class StepAssistSettings {
        public boolean autoEnable = false;
        public double height = 1.25;
    }

    public static final class SwimAssistSettings {
        public boolean autoEnable = false;
        public double horizontalBoost = 0.08;
        public double verticalBoost = 0.05;
    }

    public static final class PlayerEspSettings {
        public boolean autoEnable = false;
    }

    public static final class AutoToolSettings {
        public boolean autoEnable = false;
    }

    public static final class ChestStealerSettings {
        public boolean autoEnable = false;
    }

    public static final class SafeWalkSettings {
        public boolean autoEnable = false;
    }

    public static final class NoSlowSettings {
        public boolean autoEnable = false;
    }

    public static final class NoPushSettings {
        public boolean autoEnable = false;
    }

    public static final class NoWebSettings {
        public boolean autoEnable = false;
    }

    public static final class CreativeAccessSettings {
        public boolean autoEnable = false;
        public boolean openOnEnable = true;
        public boolean replaceInventoryScreen = false;
    }

    public static final class CameraSettings {
        public int maxSavedPoints = 16;
    }

    public static final class MinimapSettings {
        public boolean startEnabled = false;
        public int size = 128;
        public double range = 96.0;
        public boolean showTerrain = true;
        public boolean showPlayers = true;
    }

    public static final class ProtectionSettings {
        public boolean invulnerable = true;
        public boolean blockDrowningDamage = true;
        public boolean blockFallDamage = true;
        public boolean blockFreezeDamage = true;
        public boolean blockFireDamage = true;
        public boolean keepInventory = true;
        public boolean blockPvpDamage = true;
    }

    public static final class AnchorPulseSettings {
        public boolean persistAnchorBetweenSessions = true;
        public boolean showExactCoordinates = true;
    }

    public static final class PersistedAnchor {
        public boolean hasAnchor = false;
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
    }
}
