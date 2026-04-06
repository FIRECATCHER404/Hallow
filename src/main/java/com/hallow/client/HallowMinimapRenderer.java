package com.hallow.client;

import java.util.List;

import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;
import com.hallow.client.config.HallowStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

public final class HallowMinimapRenderer {
    private static final int PANEL_MARGIN = 10;
    private static final int FRAME_THICKNESS = 3;
    private static final int DROP_SHADOW = 2;
    private static final int LABEL_HEIGHT = 11;
    private static final int CACHE_TICKS = 12;

    private static final int FRAME_OUTER = 0xFF5A5140;
    private static final int FRAME_INNER = 0xFFE8DEBB;
    private static final int FRAME_SHADOW = 0x55000000;
    private static final int LABEL_BG = 0xAA12171E;
    private static final int LABEL_TEXT = 0xFFE4E8EE;
    private static final int UNLOADED_COLOR = 0xFF111921;
    private static final int PLAYER_MARKER = 0xFF82E4FF;
    private static final int SELECTED_MARKER = 0xFFFFD15B;
    private static final int SELF_MARKER = 0xFFFF4C3C;

    private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
    private boolean visible;
    private int[] terrainColors = new int[0];
    private int cachedSamples;
    private long lastSampleTick = Long.MIN_VALUE;
    private double lastCenterX = Double.NaN;
    private double lastCenterZ = Double.NaN;
    private double lastRange = Double.NaN;
    private int lastMapSize = -1;
    private String lastDimension = "";

    public void setVisible(boolean visible) {
        this.visible = visible;
        clearCache();
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggle(Minecraft client) {
        visible = !visible;
        clearCache();
        HallowStorage.markDirty();
        announce(client, visible ? "Minimap enabled." : "Minimap hidden.");
    }

    public void reset() {
        visible = false;
        clearCache();
    }

    public void render(GuiGraphics graphics, Minecraft client) {
        if (!visible || client.player == null || client.level == null) {
            return;
        }

        HallowConfig.MinimapSettings config = HallowConfigManager.get().minimap;
        int mapSize = config.size;
        int frameSize = mapSize + (FRAME_THICKNESS * 2);
        int left = client.getWindow().getGuiScaledWidth() - frameSize - PANEL_MARGIN;
        int top = PANEL_MARGIN;
        int mapLeft = left + FRAME_THICKNESS;
        int mapTop = top + FRAME_THICKNESS;
        int centerX = mapLeft + (mapSize / 2);
        int centerY = mapTop + (mapSize / 2);
        int samples = Math.min(56, Math.max(32, mapSize / 3));

        ensureTerrainCache(client, config, samples);

        graphics.fill(left + DROP_SHADOW, top + DROP_SHADOW, left + frameSize + DROP_SHADOW, top + frameSize + DROP_SHADOW, FRAME_SHADOW);
        graphics.fill(left, top, left + frameSize, top + frameSize, FRAME_OUTER);
        graphics.fill(left + 1, top + 1, left + frameSize - 1, top + frameSize - 1, FRAME_INNER);
        graphics.fill(mapLeft, mapTop, mapLeft + mapSize, mapTop + mapSize, UNLOADED_COLOR);

        drawTerrain(graphics, mapLeft, mapTop, mapSize, samples);
        drawPlayerMarkers(graphics, client, config, mapLeft, mapTop, mapSize, centerX, centerY);
        drawSelfMarker(graphics, client.player, centerX, centerY);
        drawLabel(graphics, client, mapLeft, mapTop, mapSize, config.range);
    }

    private void ensureTerrainCache(Minecraft client, HallowConfig.MinimapSettings config, int samples) {
        if (!config.showTerrain) {
            terrainColors = new int[0];
            cachedSamples = 0;
            return;
        }

        Player player = client.player;
        ClientLevel level = client.level;
        long gameTime = level.getGameTime();
        double centerX = player.getX();
        double centerZ = player.getZ();
        String dimension = level.dimension().identifier().toString();

        boolean stale = terrainColors.length == 0
            || cachedSamples != samples
            || lastMapSize != config.size
            || Double.compare(lastRange, config.range) != 0
            || !dimension.equals(lastDimension)
            || Math.abs(centerX - lastCenterX) >= 1.5
            || Math.abs(centerZ - lastCenterZ) >= 1.5
            || gameTime - lastSampleTick >= CACHE_TICKS;

        if (!stale) {
            return;
        }

        terrainColors = new int[samples * samples];
        cachedSamples = samples;

        double blocksPerSample = (config.range * 2.0) / samples;
        double startX = centerX - config.range;
        double startZ = centerZ - config.range;

        for (int row = 0; row < samples; row++) {
            for (int column = 0; column < samples; column++) {
                int sampleX = Mth.floor(startX + ((column + 0.5) * blocksPerSample));
                int sampleZ = Mth.floor(startZ + ((row + 0.5) * blocksPerSample));
                terrainColors[index(column, row, samples)] = sampleTerrain(level, sampleX, sampleZ);
            }
        }

        lastSampleTick = gameTime;
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        lastRange = config.range;
        lastMapSize = config.size;
        lastDimension = dimension;
    }

    private int sampleTerrain(ClientLevel level, int blockX, int blockZ) {
        samplePos.set(blockX, level.getMinY(), blockZ);
        if (!level.hasChunkAt(samplePos)) {
            return UNLOADED_COLOR;
        }

        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
        samplePos.set(blockX, Math.max(level.getMinY(), height - 1), blockZ);
        BlockState state = level.getBlockState(samplePos);
        if (state.isAir()) {
            return UNLOADED_COLOR;
        }

        MapColor mapColor = state.getMapColor(level, samplePos);
        if (mapColor == MapColor.NONE) {
            return UNLOADED_COLOR;
        }

        int northHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ - 1);
        int southHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ + 1);
        MapColor.Brightness brightness = MapColor.Brightness.NORMAL;
        if (northHeight < height - 2 || southHeight < height - 2) {
            brightness = MapColor.Brightness.HIGH;
        } else if (northHeight > height + 2 || southHeight > height + 2) {
            brightness = MapColor.Brightness.LOW;
        }

        int color = mapColor.calculateARGBColor(brightness);
        if (state.getFluidState().is(FluidTags.WATER)) {
            color = tint(color, 0.92F);
        } else if (state.getFluidState().is(FluidTags.LAVA)) {
            color = tint(color, 1.04F);
        }
        return color;
    }

    private void drawTerrain(GuiGraphics graphics, int left, int top, int size, int samples) {
        if (terrainColors.length == 0 || samples <= 0) {
            return;
        }

        double cellSize = size / (double) samples;
        for (int row = 0; row < samples; row++) {
            int y0 = top + Mth.floor(row * cellSize);
            int y1 = top + Mth.floor((row + 1) * cellSize);
            for (int column = 0; column < samples; column++) {
                int x0 = left + Mth.floor(column * cellSize);
                int x1 = left + Mth.floor((column + 1) * cellSize);
                graphics.fill(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), terrainColors[index(column, row, samples)]);
            }
        }
    }

    private void drawPlayerMarkers(GuiGraphics graphics, Minecraft client, HallowConfig.MinimapSettings config, int mapLeft, int mapTop, int mapSize, int centerX, int centerY) {
        if (!config.showPlayers) {
            return;
        }

        double pixelsPerBlock = (mapSize / 2.0) / config.range;
        String selectedName = null;
        HallowCameraController.TargetHudState selected = HallowCameraController.currentTargetHudState(client);
        if (selected != null) {
            selectedName = selected.name();
        }

        List<HallowCameraController.TrackedPlayerState> targets = HallowCameraController.minimapTargets(client);
        for (HallowCameraController.TrackedPlayerState target : targets) {
            Vec3 position = target.position();
            double deltaX = position.x - client.player.getX();
            double deltaZ = position.z - client.player.getZ();
            double distance = Math.hypot(deltaX, deltaZ);
            if (distance > config.range) {
                continue;
            }

            int markerX = centerX + Mth.floor(deltaX * pixelsPerBlock);
            int markerY = centerY + Mth.floor(deltaZ * pixelsPerBlock);
            int color = selectedName != null && selectedName.equalsIgnoreCase(target.name()) ? SELECTED_MARKER : PLAYER_MARKER;
            drawDiamond(graphics, markerX, markerY, color);
        }
    }

    private void drawSelfMarker(GuiGraphics graphics, Player player, int centerX, int centerY) {
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, SELF_MARKER);

        float yawRadians = player.getYRot() * Mth.DEG_TO_RAD;
        int tipX = centerX + Mth.floor(-Mth.sin(yawRadians) * 8.0F);
        int tipY = centerY + Mth.floor(Mth.cos(yawRadians) * 8.0F);

        drawLine(graphics, centerX, centerY, tipX, tipY, SELF_MARKER);
    }

    private void drawLabel(GuiGraphics graphics, Minecraft client, int mapLeft, int mapTop, int mapSize, double range) {
        String text = DirectionUtil.formatDistance(range) + "r";
        int width = client.font.width(text) + 8;
        int left = mapLeft + mapSize - width - 3;
        int top = mapTop + mapSize - LABEL_HEIGHT - 3;
        graphics.fill(left, top, left + width, top + LABEL_HEIGHT, LABEL_BG);
        graphics.drawString(client.font, text, left + 4, top + 2, LABEL_TEXT, false);
    }

    private void drawDiamond(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y - 2, x + 1, y + 3, color);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        graphics.fill(x - 2, y, x + 3, y + 1, color);
    }

    private void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps <= 0) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }

        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            int x = x0 + Mth.floor((x1 - x0) * progress);
            int y = y0 + Mth.floor((y1 - y0) * progress);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void clearCache() {
        terrainColors = new int[0];
        cachedSamples = 0;
        lastSampleTick = Long.MIN_VALUE;
        lastCenterX = Double.NaN;
        lastCenterZ = Double.NaN;
        lastRange = Double.NaN;
        lastMapSize = -1;
        lastDimension = "";
    }

    private int tint(int color, float multiplier) {
        int alpha = color >>> 24;
        int red = Mth.clamp((int) (((color >>> 16) & 0xFF) * multiplier), 0, 255);
        int green = Mth.clamp((int) (((color >>> 8) & 0xFF) * multiplier), 0, 255);
        int blue = Mth.clamp((int) ((color & 0xFF) * multiplier), 0, 255);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int index(int column, int row, int samples) {
        return (row * samples) + column;
    }

    private void announce(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[Hallow] " + message), true);
        }
    }
}
