package com.hallow.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.hallow.client.config.HallowStorage;
import org.lwjgl.glfw.GLFW;

import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.mixin.CameraAccessor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HallowCameraController {
    private static final int SAVE_KEY = GLFW.GLFW_KEY_B;
    private static final int CYCLE_KEY = GLFW.GLFW_KEY_N;
    private static final int PLAYER_WHEEL_KEY = GLFW.GLFW_KEY_M;
    private static final int PLAYER_LOCK_KEY = GLFW.GLFW_KEY_L;
    private static final int PLAYER_FOLLOW_KEY = GLFW.GLFW_KEY_K;

    private static final List<CameraPoint> SAVED_POINTS = new ArrayList<>();
    private static final Map<String, CameraPoint> PLAYER_SNAPSHOTS = new LinkedHashMap<>();

    private static Mode mode = Mode.NONE;
    private static int savedPointIndex = -1;
    private static int playerIndex = -1;
    private static boolean savePressed;
    private static boolean cyclePressed;
    private static boolean playerWheelHeld;
    private static boolean playerLockPressed;
    private static boolean playerFollowPressed;
    private static BrowseTarget currentBrowseTarget;
    private static CameraPoint lockedCameraPoint;
    private static String lockedTargetName;

    private HallowCameraController() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.getWindow() == null) {
            reset(client);
            return;
        }

        long window = client.getWindow().handle();
        boolean allowDirectKeys = client.screen == null;
        updatePlayerSnapshots(client);

        boolean saveDown = allowDirectKeys && GLFW.glfwGetKey(window, SAVE_KEY) == GLFW.GLFW_PRESS;
        if (saveDown && !savePressed) {
            if (client.player.isShiftKeyDown()) {
                clearCurrentDimensionPoints(client);
            } else {
                saveCurrentView(client);
            }
        }
        savePressed = saveDown;

        boolean cycleDown = allowDirectKeys && GLFW.glfwGetKey(window, CYCLE_KEY) == GLFW.GLFW_PRESS;
        if (cycleDown && !cyclePressed) {
            cycleSavedView(client);
        }
        cyclePressed = cycleDown;

        boolean browseDown = allowDirectKeys && GLFW.glfwGetKey(window, PLAYER_WHEEL_KEY) == GLFW.GLFW_PRESS;
        if (browseDown && !playerWheelHeld) {
            if (mode == Mode.PLAYER_BROWSE) {
                exitPlayerBrowse(client, "Returned to player camera.");
            } else {
                enterPlayerBrowse(client);
            }
        }
        playerWheelHeld = browseDown;

        if (mode == Mode.PLAYER_BROWSE) {
            ensurePlayerBrowseTarget(client);
        }

        boolean lockDown = allowDirectKeys && GLFW.glfwGetKey(window, PLAYER_LOCK_KEY) == GLFW.GLFW_PRESS;
        if (lockDown && !playerLockPressed) {
            if (mode == Mode.PLAYER_BROWSE) {
                lockJumpToCurrentTarget(client);
            } else if (mode == Mode.LOCKED_TARGET) {
                exitLockedView(client, "Returned to player camera.");
            }
        }
        playerLockPressed = lockDown;

        boolean followDown = allowDirectKeys && GLFW.glfwGetKey(window, PLAYER_FOLLOW_KEY) == GLFW.GLFW_PRESS;
        if (followDown && !playerFollowPressed) {
            if (mode == Mode.PLAYER_BROWSE) {
                followCurrentTarget(client);
            } else if (mode == Mode.FOLLOW_TARGET) {
                exitLockedView(client, "Returned to player camera.");
            }
        }
        playerFollowPressed = followDown;

        if ((mode == Mode.SAVED_POINT || mode == Mode.LOCKED_TARGET || mode == Mode.FOLLOW_TARGET) && currentFixedPoint(client) == null) {
            if (mode == Mode.SAVED_POINT) {
                exitSavedView(client);
            } else {
                exitLockedView(client, "Camera target is no longer available.");
            }
        }
    }

    public static boolean handleScroll(Minecraft client, double verticalAmount) {
        if (client == null || mode != Mode.PLAYER_BROWSE || verticalAmount == 0.0) {
            return false;
        }

        List<BrowseTarget> targets = browseableTargets(client);
        if (targets.isEmpty()) {
            exitPlayerBrowse(client, "No player cameras are available.");
            return true;
        }

        int direction = verticalAmount > 0.0 ? -1 : 1;
        playerIndex = Math.floorMod(playerIndex + direction, targets.size());
        applyBrowseTarget(client, targets);
        return true;
    }

    public static List<String> hudLines(Minecraft client) {
        if (mode == Mode.PLAYER_BROWSE && client != null) {
            List<BrowseTarget> targets = browseableTargets(client);
            if (!targets.isEmpty() && playerIndex >= 0 && playerIndex < targets.size()) {
                BrowseTarget target = targets.get(playerIndex);
                return List.of(
                    "Camera: follow " + target.name() + " [" + (playerIndex + 1) + "/" + targets.size() + "] " + (target.livePlayer() != null ? "LIVE" : "SNAPSHOT"),
                    "Scroll cycle | L lock | K follow | H/J copy"
                );
            }
        }

        if (mode == Mode.SAVED_POINT && client != null) {
            List<Integer> points = savedPointIndicesForCurrentDimension(client);
            int current = Math.max(0, points.indexOf(savedPointIndex));
            return List.of(
                "Camera: saved view [" + (current + 1) + "/" + points.size() + "]",
                "Press N for next saved camera"
            );
        }

        if (mode == Mode.LOCKED_TARGET && lockedCameraPoint != null) {
            return List.of(
                "Camera: jump " + lockedTargetName,
                "Press L to return to player"
            );
        }

        if (mode == Mode.FOLLOW_TARGET && client != null) {
            BrowseTarget target = currentFollowTarget(client);
            if (target != null) {
                return List.of(
                    "Camera: follow " + target.name() + " " + (target.livePlayer() != null ? "LIVE" : "SNAPSHOT"),
                    "Press K to return to player"
                );
            }
        }

        if (!SAVED_POINTS.isEmpty()) {
            return List.of("Camera: " + SAVED_POINTS.size() + " saved | B save | N cycle | Hold M players");
        }

        return List.of();
    }

    public static boolean isFixedViewActive() {
        return mode == Mode.SAVED_POINT;
    }

    public static CameraPoint currentFixedPoint(Minecraft client) {
        if (client == null || client.level == null) {
            return null;
        }

        return switch (mode) {
            case SAVED_POINT -> {
                if (savedPointIndex < 0 || savedPointIndex >= SAVED_POINTS.size()) {
                    yield null;
                }

                CameraPoint point = SAVED_POINTS.get(savedPointIndex);
                String dimension = client.level.dimension().identifier().toString();
                yield dimension.equals(point.dimension()) ? point : null;
            }
            case LOCKED_TARGET -> pointInCurrentDimension(client, lockedCameraPoint);
            case FOLLOW_TARGET -> {
                BrowseTarget target = currentFollowTarget(client);
                yield target == null ? null : pointInCurrentDimension(client, cameraPointForTarget(target));
            }
            default -> null;
        };
    }

    public static boolean isCameraDetached() {
        return mode == Mode.SAVED_POINT || mode == Mode.PLAYER_BROWSE || mode == Mode.LOCKED_TARGET || mode == Mode.FOLLOW_TARGET;
    }

    public static CameraPoint currentBrowsePoint(Minecraft client) {
        if (mode != Mode.PLAYER_BROWSE || currentBrowseTarget == null || client == null) {
            return null;
        }

        return pointInCurrentDimension(client, cameraPointForTarget(currentBrowseTarget));
    }

    public static TargetHudState currentTargetHudState(Minecraft client) {
        BrowseTarget target = currentSelectedTarget(client);

        if (target == null) {
            return null;
        }

        return new TargetHudState(target.name(), target.livePlayer(), target.snapshot() != null && target.livePlayer() == null);
    }

    public static Player currentLiveTarget(Minecraft client) {
        BrowseTarget target = currentSelectedTarget(client);
        return target == null ? null : target.livePlayer();
    }

    public static List<CameraPoint> exportSavedPoints() {
        return List.copyOf(SAVED_POINTS);
    }

    public static void loadSavedPoints(List<CameraPoint> points) {
        SAVED_POINTS.clear();
        savedPointIndex = -1;
        if (points == null) {
            return;
        }

        int maxPoints = HallowConfigManager.get().camera.maxSavedPoints;
        for (CameraPoint point : points) {
            if (point != null && point.dimension() != null && !point.dimension().isBlank()) {
                SAVED_POINTS.add(point);
            }
        }

        while (SAVED_POINTS.size() > maxPoints) {
            SAVED_POINTS.removeFirst();
        }
    }

    public static List<TrackedPlayerState> minimapTargets(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return List.of();
        }

        List<TrackedPlayerState> targets = new ArrayList<>();
        for (Player player : client.level.players()) {
            if (player == client.player) {
                continue;
            }

            String name = player.getGameProfile().name();
            if (name == null || name.isBlank()) {
                continue;
            }

            targets.add(new TrackedPlayerState(name, player.getEyePosition(), true));
        }

        targets.sort(Comparator.comparing(TrackedPlayerState::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(targets);
    }

    public static void reset(Minecraft client) {
        SAVED_POINTS.clear();
        PLAYER_SNAPSHOTS.clear();
        savedPointIndex = -1;
        playerIndex = -1;
        savePressed = false;
        cyclePressed = false;
        playerWheelHeld = false;
        playerLockPressed = false;
        playerFollowPressed = false;
        mode = Mode.NONE;
        currentBrowseTarget = null;
        lockedCameraPoint = null;
        lockedTargetName = null;
        restorePlayerCamera(client);
    }

    private static void saveCurrentView(Minecraft client) {
        CameraPoint point;
        String sourceLabel = null;

        if (mode == Mode.PLAYER_BROWSE && currentBrowseTarget != null) {
            point = cameraPointForTarget(currentBrowseTarget);
            sourceLabel = currentBrowseTarget.name();
        } else if ((mode == Mode.LOCKED_TARGET || mode == Mode.FOLLOW_TARGET) && currentFixedPoint(client) != null) {
            point = currentFixedPoint(client);
            sourceLabel = lockedTargetName;
        } else {
            Camera camera = client.gameRenderer.getMainCamera();
            CameraAccessor accessor = (CameraAccessor) camera;
            String dimension = client.level.dimension().identifier().toString();
            point = new CameraPoint(dimension, accessor.hallow$getPosition(), accessor.hallow$getYRot(), accessor.hallow$getXRot());
        }

        if (point == null) {
            announce(client, "Camera point is not available.");
            return;
        }

        addSavedPoint(point);
        if (sourceLabel == null || sourceLabel.isBlank()) {
            announce(client, "Camera point saved. Total: " + SAVED_POINTS.size());
        } else {
            announce(client, "Saved " + sourceLabel + " camera point. Total: " + SAVED_POINTS.size());
        }
    }

    private static void clearCurrentDimensionPoints(Minecraft client) {
        String dimension = client.level.dimension().identifier().toString();
        SAVED_POINTS.removeIf(point -> point.dimension().equals(dimension));
        HallowStorage.markDirty();
        if (mode == Mode.SAVED_POINT && currentFixedPoint(client) == null) {
            exitSavedView(client);
        }
        announce(client, "Cleared saved camera points for this dimension.");
    }

    private static void cycleSavedView(Minecraft client) {
        List<Integer> matchingPoints = savedPointIndicesForCurrentDimension(client);
        if (matchingPoints.isEmpty()) {
            exitSavedView(client);
            announce(client, "No saved camera points in this dimension.");
            return;
        }

        int nextMatchIndex = mode == Mode.SAVED_POINT ? matchingPoints.indexOf(savedPointIndex) + 1 : 0;
        if (nextMatchIndex >= matchingPoints.size()) {
            exitSavedView(client);
            announce(client, "Returned to player camera.");
            return;
        }

        mode = Mode.SAVED_POINT;
        savedPointIndex = matchingPoints.get(nextMatchIndex);
        restorePlayerCamera(client);
        announce(client, "Viewing saved camera " + (nextMatchIndex + 1) + " of " + matchingPoints.size() + ".");
    }

    private static void enterPlayerBrowse(Minecraft client) {
        List<BrowseTarget> targets = browseableTargets(client);
        if (targets.isEmpty()) {
            int onlinePlayers = onlinePlayerCount(client);
            if (onlinePlayers > 0) {
                announce(client, onlinePlayers + " online, but none are loaded on your client yet.");
            } else {
                announce(client, "No other players online.");
            }
            mode = Mode.NONE;
            return;
        }

        mode = Mode.PLAYER_BROWSE;
        playerIndex = playerIndex < 0 ? 0 : Math.floorMod(playerIndex, targets.size());
        applyBrowseTarget(client, targets);
        announce(client, "Following " + targets.get(playerIndex).name() + ". Scroll to cycle, press M to exit.");
    }

    private static void ensurePlayerBrowseTarget(Minecraft client) {
        List<BrowseTarget> targets = browseableTargets(client);
        if (targets.isEmpty()) {
            exitPlayerBrowse(client, "No player cameras are available.");
            return;
        }

        playerIndex = Math.floorMod(playerIndex, targets.size());
        applyBrowseTarget(client, targets);
    }

    private static void applyBrowseTarget(Minecraft client, List<BrowseTarget> targets) {
        BrowseTarget target = targets.get(playerIndex);
        currentBrowseTarget = target;
        restorePlayerCamera(client);
    }

    private static void exitPlayerBrowse(Minecraft client, String message) {
        if (mode == Mode.PLAYER_BROWSE) {
            mode = Mode.NONE;
        }
        playerIndex = -1;
        currentBrowseTarget = null;
        restorePlayerCamera(client);
        if (message != null) {
            announce(client, message);
        }
    }

    private static void exitSavedView(Minecraft client) {
        if (mode == Mode.SAVED_POINT) {
            mode = Mode.NONE;
        }
        savedPointIndex = -1;
        restorePlayerCamera(client);
    }

    private static void addSavedPoint(CameraPoint point) {
        SAVED_POINTS.add(point);
        HallowStorage.markDirty();

        int maxPoints = HallowConfigManager.get().camera.maxSavedPoints;
        while (SAVED_POINTS.size() > maxPoints) {
            SAVED_POINTS.removeFirst();
            if (savedPointIndex >= 0) {
                savedPointIndex--;
            }
        }
    }

    private static void lockJumpToCurrentTarget(Minecraft client) {
        if (currentBrowseTarget == null) {
            announce(client, "No player selected.");
            return;
        }

        CameraPoint point = cameraPointForTarget(currentBrowseTarget);
        if (point == null) {
            announce(client, "Selected player camera is not available.");
            return;
        }

        mode = Mode.LOCKED_TARGET;
        lockedCameraPoint = point;
        lockedTargetName = currentBrowseTarget.name();
        currentBrowseTarget = null;
        restorePlayerCamera(client);
        announce(client, "Jumped camera to " + lockedTargetName + ".");
    }

    private static void followCurrentTarget(Minecraft client) {
        if (currentBrowseTarget == null) {
            announce(client, "No player selected.");
            return;
        }

        mode = Mode.FOLLOW_TARGET;
        lockedTargetName = currentBrowseTarget.name();
        lockedCameraPoint = cameraPointForTarget(currentBrowseTarget);
        currentBrowseTarget = null;
        restorePlayerCamera(client);
        announce(client, "Following " + lockedTargetName + ".");
    }

    private static void exitLockedView(Minecraft client, String message) {
        if (mode != Mode.LOCKED_TARGET && mode != Mode.FOLLOW_TARGET) {
            return;
        }

        mode = Mode.NONE;
        lockedCameraPoint = null;
        lockedTargetName = null;
        restorePlayerCamera(client);
        if (message != null) {
            announce(client, message);
        }
    }

    private static void restorePlayerCamera(Minecraft client) {
        if (client != null && client.player != null) {
            client.setCameraEntity(client.player);
        }
    }

    private static List<Integer> savedPointIndicesForCurrentDimension(Minecraft client) {
        String dimension = client.level.dimension().identifier().toString();
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < SAVED_POINTS.size(); index++) {
            if (dimension.equals(SAVED_POINTS.get(index).dimension())) {
                indices.add(index);
            }
        }
        return indices;
    }

    private static List<BrowseTarget> browseableTargets(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.level == null || client.player == null) {
            return List.of();
        }

        String currentDimension = client.level.dimension().identifier().toString();
        List<BrowseTarget> resolvedPlayers = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String profileName = info.getProfile().name();
            if (profileName == null || profileName.equalsIgnoreCase(client.player.getGameProfile().name())) {
                continue;
            }

            Player matchingPlayer = client.level.players().stream()
                .filter(player -> player.getGameProfile().name().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(null);

            CameraPoint snapshot = PLAYER_SNAPSHOTS.get(profileName.toLowerCase(Locale.ROOT));
            if (snapshot != null && !snapshot.dimension().equals(currentDimension)) {
                snapshot = null;
            }

            if (matchingPlayer != null || snapshot != null) {
                resolvedPlayers.add(new BrowseTarget(profileName, matchingPlayer, snapshot));
            }
        }

        resolvedPlayers.sort(Comparator.comparing(BrowseTarget::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(resolvedPlayers);
    }

    private static int onlinePlayerCount(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.player == null) {
            return 0;
        }

        int count = 0;
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String profileName = info.getProfile().name();
            if (profileName != null && !profileName.equalsIgnoreCase(client.player.getGameProfile().name())) {
                count++;
            }
        }
        return count;
    }

    private static void updatePlayerSnapshots(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }

        String dimension = client.level.dimension().identifier().toString();
        for (Player player : client.level.players()) {
            if (player == client.player) {
                continue;
            }

            String name = player.getGameProfile().name();
            if (name == null || name.isEmpty()) {
                continue;
            }

            PLAYER_SNAPSHOTS.put(
                name.toLowerCase(Locale.ROOT),
                new CameraPoint(dimension, player.getEyePosition(), player.getYRot(), player.getXRot())
            );
        }

        while (PLAYER_SNAPSHOTS.size() > 128) {
            String firstKey = PLAYER_SNAPSHOTS.keySet().iterator().next();
            PLAYER_SNAPSHOTS.remove(firstKey);
        }
    }

    private static BrowseTarget currentFollowTarget(Minecraft client) {
        if (mode != Mode.FOLLOW_TARGET || lockedTargetName == null) {
            return null;
        }

        return resolveTargetByName(client, lockedTargetName, lockedCameraPoint);
    }

    private static BrowseTarget currentSelectedTarget(Minecraft client) {
        return switch (mode) {
            case PLAYER_BROWSE -> currentBrowseTarget;
            case FOLLOW_TARGET -> currentFollowTarget(client);
            case LOCKED_TARGET -> resolveTargetByName(client, lockedTargetName, lockedCameraPoint);
            default -> null;
        };
    }

    private static BrowseTarget resolveTargetByName(Minecraft client, String targetName, CameraPoint fallbackPoint) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }

        if (client == null) {
            return fallbackPoint == null ? null : new BrowseTarget(targetName, null, fallbackPoint);
        }

        for (BrowseTarget target : browseableTargets(client)) {
            if (target.name().equalsIgnoreCase(targetName)) {
                return target;
            }
        }

        return fallbackPoint == null ? null : new BrowseTarget(targetName, null, fallbackPoint);
    }

    private static CameraPoint pointInCurrentDimension(Minecraft client, CameraPoint point) {
        if (point == null) {
            return null;
        }

        String dimension = client.level.dimension().identifier().toString();
        return dimension.equals(point.dimension()) ? point : null;
    }

    private static CameraPoint cameraPointForTarget(BrowseTarget target) {
        if (target == null) {
            return null;
        }

        if (target.livePlayer() != null) {
            return new CameraPoint(
                target.livePlayer().level().dimension().identifier().toString(),
                target.livePlayer().getEyePosition(),
                target.livePlayer().getYRot(),
                target.livePlayer().getXRot()
            );
        }

        return target.snapshot();
    }

    private static void announce(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("[Hallow] " + message), true);
        }
    }

    private enum Mode {
        NONE,
        SAVED_POINT,
        PLAYER_BROWSE,
        LOCKED_TARGET,
        FOLLOW_TARGET
    }

    private record BrowseTarget(String name, Player livePlayer, CameraPoint snapshot) {
    }

    public record TargetHudState(String name, Player livePlayer, boolean snapshotOnly) {
    }

    public record TrackedPlayerState(String name, Vec3 position, boolean live) {
    }

    public record CameraPoint(String dimension, Vec3 position, float yaw, float pitch) {
    }
}
