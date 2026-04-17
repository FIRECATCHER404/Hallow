package com.hallow.client.config;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import com.hallow.client.HallowRuntimeState;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class HallowConfigScreen extends Screen {
    private static final int BASE_PANEL_WIDTH = 476;
    private static final int MIN_PANEL_WIDTH = 200;
    private static final int ROW_HEIGHT = 22;
    private static final int COLUMN_GAP = 14;
    private static final int TAB_GAP = 8;
    private static final int SCROLL_STEP = 24;

    private final Screen previous;
    private final List<LayoutWidget> pageWidgets = new ArrayList<>();
    private HallowConfig workingCopy;
    private Category category = Category.VISION;
    private EditBox xRayBlocksBox;
    private EditBox xRayResourcePackBox;
    private int scrollOffset;
    private int maxScroll;
    private int contentHeight;

    public HallowConfigScreen(Screen previous) {
        super(Component.literal("Hallow Control Deck"));
        this.previous = previous;
        this.workingCopy = HallowConfigManager.get().copy();
    }

    @Override
    protected void init() {
        clearWidgets();
        pageWidgets.clear();
        xRayBlocksBox = null;
        xRayResourcePackBox = null;
        contentHeight = 0;

        int tabY = panelTop() + 36;
        int tabLeft = panelLeft() + 8;
        int tabsPerRow = tabsPerRow();

        for (int index = 0; index < Category.values().length; index++) {
            Category value = Category.values()[index];
            int row = index / tabsPerRow;
            int column = index % tabsPerRow;
            int rowItemCount = Math.min(tabsPerRow, Category.values().length - (row * tabsPerRow));
            int tabWidth = tabWidth(rowItemCount);
            Button button = Button.builder(value.labelComponent(), press -> {
                category = value;
                scrollOffset = 0;
                rebuildWidgets();
            }).bounds(
                tabLeft + (column * (tabWidth + TAB_GAP)),
                tabY + (row * (20 + TAB_GAP)),
                tabWidth,
                20
            ).build();
            button.active = value != category;
            addRenderableWidget(button);
        }

        int pageLeft = contentLeft();
        int pageTop = contentTop() + 10;
        int pageBottom;

        switch (category) {
            case VISION -> pageBottom = addVisionPage(pageLeft, pageTop);
            case TRAVERSAL -> pageBottom = addTraversalPage(pageLeft, pageTop);
            case AWARENESS -> pageBottom = addAwarenessPage(pageLeft, pageTop);
            case ACCESS_AND_HUD -> pageBottom = addAccessPage(pageLeft, pageTop);
            case PROTECTION -> pageBottom = addProtectionPage(pageLeft, pageTop);
            default -> pageBottom = pageTop;
        }

        contentHeight = Math.max(0, pageBottom - (contentTop() + 8));
        updateScrollBounds();

        int buttonY = footerTop();
        if (stackedFooter()) {
            int buttonWidth = panelWidth() - 20;
            addRenderableWidget(
                Button.builder(Component.literal("Save"), button -> saveAndClose())
                    .bounds(contentLeft(), buttonY, buttonWidth, 20)
                    .build()
            );
            addRenderableWidget(
                Button.builder(Component.literal("Restore Defaults"), button -> {
                    workingCopy = new HallowConfig();
                    rebuildWidgets();
                }).bounds(contentLeft(), buttonY + 24, buttonWidth, 20).build()
            );
            addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                    .bounds(contentLeft(), buttonY + 48, buttonWidth, 20)
                    .build()
            );
        } else {
            int buttonWidth = Math.max(84, Math.min(116, (panelWidth() - 40 - (2 * TAB_GAP)) / 3));
            int buttonsLeft = panelLeft() + (panelWidth() - ((buttonWidth * 3) + (TAB_GAP * 2))) / 2;

            addRenderableWidget(
                Button.builder(Component.literal("Save"), button -> saveAndClose())
                    .bounds(buttonsLeft, buttonY, buttonWidth, 20)
                    .build()
            );
            addRenderableWidget(
                Button.builder(Component.literal("Restore Defaults"), button -> {
                    workingCopy = new HallowConfig();
                    rebuildWidgets();
                }).bounds(buttonsLeft + buttonWidth + TAB_GAP, buttonY, buttonWidth, 20).build()
            );
            addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                    .bounds(buttonsLeft + ((buttonWidth + TAB_GAP) * 2), buttonY, buttonWidth, 20)
                    .build()
            );
        }

        applyScroll();
        if (xRayBlocksBox != null) {
            setInitialFocus(xRayBlocksBox);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (!isInsideContent(mouseX, mouseY) || maxScroll <= 0 || verticalAmount == 0.0) {
            return false;
        }

        scrollOffset = clamp(scrollOffset - ((int) Math.signum(verticalAmount) * SCROLL_STEP), 0, maxScroll);
        applyScroll();
        return true;
    }

    private int addVisionPage(int left, int y) {
        if (compactLayout()) {
            int width = contentWidth();
            int bottom = y;

            addPageWidget(toggle(left, bottom, "Fullbright Auto-Enable", workingCopy.fullbright.autoEnable, value -> workingCopy.fullbright.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "X-Ray Auto-Enable", workingCopy.xray.autoEnable, value -> workingCopy.xray.autoEnable = value));
            bottom += 24;
            addPageWidget(
                CycleButton.builder(mode -> Component.literal(mode.label()), workingCopy.xray.mode)
                    .withValues(List.of(XRayMode.values()))
                    .create(left, bottom, width, 20, Component.literal("X-Ray Mode"), (button, value) -> workingCopy.xray.mode = value)
            );
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Horizontal Radius", 4, 24, workingCopy.xray.horizontalRadius, value -> workingCopy.xray.horizontalRadius = value, value -> Integer.toString(value)));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Vertical Radius", 4, 16, workingCopy.xray.verticalRadius, value -> workingCopy.xray.verticalRadius = value, value -> Integer.toString(value)));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Max Targets", 16, 256, workingCopy.xray.maxTargets, value -> workingCopy.xray.maxTargets = value, value -> Integer.toString(value)));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Scan Interval", 4, 40, workingCopy.xray.scanInterval, value -> workingCopy.xray.scanInterval = value, value -> value + "t"));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Spectator Peek", 1.0, 8.0, workingCopy.xray.spectatorPeekDistance, value -> workingCopy.xray.spectatorPeekDistance = value, value -> format(value, 1) + "b"));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Spectator Push", 0.05, 1.5, workingCopy.xray.spectatorPush, value -> workingCopy.xray.spectatorPush = value, value -> format(value, 2) + "b"));
            bottom += 30;

            xRayBlocksBox = addPageWidget(new EditBox(this.font, left, bottom, width, 20, Component.literal("X-Ray block ids")));
            xRayBlocksBox.setMaxLength(2048);
            xRayBlocksBox.setValue(String.join(", ", workingCopy.xray.trackedBlocks));
            xRayBlocksBox.setHint(Component.literal("diamond_ore, chest, ancient_debris"));
            bottom += 24;
            addPageWidget(
                Button.builder(Component.literal("Ore Defaults"), button -> {
                    workingCopy.xray.trackedBlocks = HallowConfig.defaultXRayBlockIds();
                    if (xRayBlocksBox != null) {
                        xRayBlocksBox.setValue(String.join(", ", workingCopy.xray.trackedBlocks));
                    }
                }).bounds(left, bottom, width, 20).build()
            );
            bottom += 30;
            xRayResourcePackBox = addPageWidget(new EditBox(this.font, left, bottom, width, 20, Component.literal("Custom X-Ray pack override")));
            xRayResourcePackBox.setMaxLength(2048);
            xRayResourcePackBox.setValue(workingCopy.xray.resourcePackPath);
            xRayResourcePackBox.setHint(Component.literal("Optional: C:\\Users\\you\\Downloads\\XRAy.zip"));
            bottom += 30;
            addPageWidget(toggle(left, bottom, "NoRender Auto-Enable", workingCopy.noRender.autoEnable, value -> workingCopy.noRender.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Hide Fog", workingCopy.noRender.hideFog, value -> workingCopy.noRender.hideFog = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Hide Camera Overlays", workingCopy.noRender.hideCameraOverlays, value -> workingCopy.noRender.hideCameraOverlays = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Hide View Bobbing", workingCopy.noRender.hideViewBobbing, value -> workingCopy.noRender.hideViewBobbing = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Hide Damage Tilt", workingCopy.noRender.hideDamageTilt, value -> workingCopy.noRender.hideDamageTilt = value));

            return bottom + 24;
        }

        int right = rightColumnLeft();
        int columnWidth = columnWidth();
        int leftBottom = y;
        int rightBottom = y;

        addPageWidget(toggle(left, leftBottom, "Fullbright Auto-Enable", workingCopy.fullbright.autoEnable, value -> workingCopy.fullbright.autoEnable = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "X-Ray Auto-Enable", workingCopy.xray.autoEnable, value -> workingCopy.xray.autoEnable = value));
        leftBottom += 24;
        addPageWidget(
            CycleButton.builder(mode -> Component.literal(mode.label()), workingCopy.xray.mode)
                .withValues(List.of(XRayMode.values()))
                .create(left, leftBottom, columnWidth, 20, Component.literal("X-Ray Mode"), (button, value) -> workingCopy.xray.mode = value)
        );
        leftBottom += 30;

        addPageWidget(new IntSlider(right, rightBottom, columnWidth, "Horizontal Radius", 4, 24, workingCopy.xray.horizontalRadius, value -> workingCopy.xray.horizontalRadius = value, value -> Integer.toString(value)));
        rightBottom += 24;
        addPageWidget(new IntSlider(right, rightBottom, columnWidth, "Vertical Radius", 4, 16, workingCopy.xray.verticalRadius, value -> workingCopy.xray.verticalRadius = value, value -> Integer.toString(value)));
        rightBottom += 24;
        addPageWidget(new IntSlider(right, rightBottom, columnWidth, "Max Targets", 16, 256, workingCopy.xray.maxTargets, value -> workingCopy.xray.maxTargets = value, value -> Integer.toString(value)));
        rightBottom += 24;
        addPageWidget(new IntSlider(right, rightBottom, columnWidth, "Scan Interval", 4, 40, workingCopy.xray.scanInterval, value -> workingCopy.xray.scanInterval = value, value -> value + "t"));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Spectator Peek", 1.0, 8.0, workingCopy.xray.spectatorPeekDistance, value -> workingCopy.xray.spectatorPeekDistance = value, value -> format(value, 1) + "b"));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Spectator Push", 0.05, 1.5, workingCopy.xray.spectatorPush, value -> workingCopy.xray.spectatorPush = value, value -> format(value, 2) + "b"));
        rightBottom += 30;

        int blockRowY = Math.max(leftBottom, rightBottom);
        int textWidth = Math.max(170, panelWidth() - 148);
        xRayBlocksBox = addPageWidget(new EditBox(this.font, left, blockRowY, textWidth, 20, Component.literal("X-Ray block ids")));
        xRayBlocksBox.setMaxLength(2048);
        xRayBlocksBox.setValue(String.join(", ", workingCopy.xray.trackedBlocks));
        xRayBlocksBox.setHint(Component.literal("diamond_ore, chest, ancient_debris"));
        addPageWidget(
            Button.builder(Component.literal("Ore Defaults"), button -> {
                workingCopy.xray.trackedBlocks = HallowConfig.defaultXRayBlockIds();
                if (xRayBlocksBox != null) {
                    xRayBlocksBox.setValue(String.join(", ", workingCopy.xray.trackedBlocks));
                }
            }).bounds(left + textWidth + 10, blockRowY, panelRight() - (left + textWidth + 10) - 10, 20).build()
        );
        int packRowY = blockRowY + 34;
        xRayResourcePackBox = addPageWidget(new EditBox(this.font, left, packRowY, contentWidth(), 20, Component.literal("Custom X-Ray pack override")));
        xRayResourcePackBox.setMaxLength(2048);
        xRayResourcePackBox.setValue(workingCopy.xray.resourcePackPath);
        xRayResourcePackBox.setHint(Component.literal("Optional: C:\\Users\\you\\Downloads\\XRAy.zip"));
        int noRenderLeftY = packRowY + 34;
        int noRenderRightY = packRowY + 34;

        addPageWidget(toggle(left, noRenderLeftY, "NoRender Auto-Enable", workingCopy.noRender.autoEnable, value -> workingCopy.noRender.autoEnable = value));
        noRenderLeftY += 24;
        addPageWidget(toggle(left, noRenderLeftY, "Hide Fog", workingCopy.noRender.hideFog, value -> workingCopy.noRender.hideFog = value));
        noRenderLeftY += 24;
        addPageWidget(toggle(left, noRenderLeftY, "Hide Camera Overlays", workingCopy.noRender.hideCameraOverlays, value -> workingCopy.noRender.hideCameraOverlays = value));

        addPageWidget(toggle(right, noRenderRightY, "Hide View Bobbing", workingCopy.noRender.hideViewBobbing, value -> workingCopy.noRender.hideViewBobbing = value));
        noRenderRightY += 24;
        addPageWidget(toggle(right, noRenderRightY, "Hide Damage Tilt", workingCopy.noRender.hideDamageTilt, value -> workingCopy.noRender.hideDamageTilt = value));

        return Math.max(noRenderLeftY, noRenderRightY) + 24;
    }

    private int addTraversalPage(int left, int y) {
        if (compactLayout()) {
            int width = contentWidth();
            int bottom = y;

            addPageWidget(toggle(left, bottom, "Fly Auto-Enable", workingCopy.fly.autoEnable, value -> workingCopy.fly.autoEnable = value));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Fly Speed", 0.05, 0.5, workingCopy.fly.speed, value -> workingCopy.fly.speed = value, value -> format(value, 2)));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Auto Sprint Auto-Enable", workingCopy.autoSprint.autoEnable, value -> workingCopy.autoSprint.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Sprint While Using Item", workingCopy.autoSprint.keepWhileUsingItem, value -> workingCopy.autoSprint.keepWhileUsingItem = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Step Assist Auto-Enable", workingCopy.stepAssist.autoEnable, value -> workingCopy.stepAssist.autoEnable = value));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Step Height", 0.6, 2.0, workingCopy.stepAssist.height, value -> workingCopy.stepAssist.height = value, value -> format(value, 2) + "b"));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Swim Assist Auto-Enable", workingCopy.swimAssist.autoEnable, value -> workingCopy.swimAssist.autoEnable = value));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Swim Horizontal Boost", 0.01, 0.2, workingCopy.swimAssist.horizontalBoost, value -> workingCopy.swimAssist.horizontalBoost = value, value -> format(value, 2)));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Swim Vertical Boost", 0.01, 0.15, workingCopy.swimAssist.verticalBoost, value -> workingCopy.swimAssist.verticalBoost = value, value -> format(value, 2)));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "AutoTool Auto-Enable", workingCopy.autoTool.autoEnable, value -> workingCopy.autoTool.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "SafeWalk Auto-Enable", workingCopy.safeWalk.autoEnable, value -> workingCopy.safeWalk.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "NoSlow Auto-Enable", workingCopy.noSlow.autoEnable, value -> workingCopy.noSlow.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "NoPush Auto-Enable", workingCopy.noPush.autoEnable, value -> workingCopy.noPush.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "NoWeb Auto-Enable", workingCopy.noWeb.autoEnable, value -> workingCopy.noWeb.autoEnable = value));

            return bottom + 24;
        }

        int right = rightColumnLeft();
        int columnWidth = columnWidth();
        int leftBottom = y;
        int rightBottom = y;

        addPageWidget(toggle(left, leftBottom, "Fly Auto-Enable", workingCopy.fly.autoEnable, value -> workingCopy.fly.autoEnable = value));
        leftBottom += 24;
        addPageWidget(new DoubleSlider(left, leftBottom, columnWidth, "Fly Speed", 0.05, 0.5, workingCopy.fly.speed, value -> workingCopy.fly.speed = value, value -> format(value, 2)));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Auto Sprint Auto-Enable", workingCopy.autoSprint.autoEnable, value -> workingCopy.autoSprint.autoEnable = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Sprint While Using Item", workingCopy.autoSprint.keepWhileUsingItem, value -> workingCopy.autoSprint.keepWhileUsingItem = value));
        leftBottom += 24;

        addPageWidget(toggle(right, rightBottom, "Step Assist Auto-Enable", workingCopy.stepAssist.autoEnable, value -> workingCopy.stepAssist.autoEnable = value));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Step Height", 0.6, 2.0, workingCopy.stepAssist.height, value -> workingCopy.stepAssist.height = value, value -> format(value, 2) + "b"));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Swim Assist Auto-Enable", workingCopy.swimAssist.autoEnable, value -> workingCopy.swimAssist.autoEnable = value));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Swim Horizontal Boost", 0.01, 0.2, workingCopy.swimAssist.horizontalBoost, value -> workingCopy.swimAssist.horizontalBoost = value, value -> format(value, 2)));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Swim Vertical Boost", 0.01, 0.15, workingCopy.swimAssist.verticalBoost, value -> workingCopy.swimAssist.verticalBoost = value, value -> format(value, 2)));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "AutoTool Auto-Enable", workingCopy.autoTool.autoEnable, value -> workingCopy.autoTool.autoEnable = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "SafeWalk Auto-Enable", workingCopy.safeWalk.autoEnable, value -> workingCopy.safeWalk.autoEnable = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "NoSlow Auto-Enable", workingCopy.noSlow.autoEnable, value -> workingCopy.noSlow.autoEnable = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "NoPush Auto-Enable", workingCopy.noPush.autoEnable, value -> workingCopy.noPush.autoEnable = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "NoWeb Auto-Enable", workingCopy.noWeb.autoEnable, value -> workingCopy.noWeb.autoEnable = value));

        return Math.max(leftBottom, rightBottom) + 24;
    }

    private int addAwarenessPage(int left, int y) {
        if (compactLayout()) {
            int width = contentWidth();
            int bottom = y;

            addPageWidget(toggle(left, bottom, "Loot Compass Auto-Enable", workingCopy.lootCompass.autoEnable, value -> workingCopy.lootCompass.autoEnable = value));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Loot Range", 8.0, 96.0, workingCopy.lootCompass.range, value -> workingCopy.lootCompass.range = value, value -> format(value, 0) + "b"));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Loot Scan Interval", 2, 40, workingCopy.lootCompass.scanInterval, value -> workingCopy.lootCompass.scanInterval = value, value -> value + "t"));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Threat Radar Auto-Enable", workingCopy.threatRadar.autoEnable, value -> workingCopy.threatRadar.autoEnable = value));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Threat Range", 8.0, 96.0, workingCopy.threatRadar.range, value -> workingCopy.threatRadar.range = value, value -> format(value, 0) + "b"));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Threat Scan Interval", 2, 40, workingCopy.threatRadar.scanInterval, value -> workingCopy.threatRadar.scanInterval = value, value -> value + "t"));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Blindside Threshold", -0.95, 0.95, workingCopy.threatRadar.blindsideThreshold, value -> workingCopy.threatRadar.blindsideThreshold = value, value -> format(value, 2)));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Glow Nearby Players Too", workingCopy.threatRadar.highlightPlayers, value -> workingCopy.threatRadar.highlightPlayers = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Player ESP Auto-Enable", workingCopy.playerEsp.autoEnable, value -> workingCopy.playerEsp.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Projectile Predict Auto-Enable", workingCopy.projectilePredict.autoEnable, value -> workingCopy.projectilePredict.autoEnable = value));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Predict Steps", 30, 240, workingCopy.projectilePredict.maxSteps, value -> workingCopy.projectilePredict.maxSteps = value, value -> Integer.toString(value)));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Predict Line Width", 1.0, 6.0, workingCopy.projectilePredict.lineWidth, value -> workingCopy.projectilePredict.lineWidth = value, value -> format(value, 1)));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Show Landing Marker", workingCopy.projectilePredict.showLandingMarker, value -> workingCopy.projectilePredict.showLandingMarker = value));

            return bottom + 24;
        }

        int right = rightColumnLeft();
        int columnWidth = columnWidth();
        int leftBottom = y;
        int rightBottom = y;

        addPageWidget(toggle(left, leftBottom, "Loot Compass Auto-Enable", workingCopy.lootCompass.autoEnable, value -> workingCopy.lootCompass.autoEnable = value));
        leftBottom += 24;
        addPageWidget(new DoubleSlider(left, leftBottom, columnWidth, "Loot Range", 8.0, 96.0, workingCopy.lootCompass.range, value -> workingCopy.lootCompass.range = value, value -> format(value, 0) + "b"));
        leftBottom += 24;
        addPageWidget(new IntSlider(left, leftBottom, columnWidth, "Loot Scan Interval", 2, 40, workingCopy.lootCompass.scanInterval, value -> workingCopy.lootCompass.scanInterval = value, value -> value + "t"));

        addPageWidget(toggle(right, rightBottom, "Threat Radar Auto-Enable", workingCopy.threatRadar.autoEnable, value -> workingCopy.threatRadar.autoEnable = value));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Threat Range", 8.0, 96.0, workingCopy.threatRadar.range, value -> workingCopy.threatRadar.range = value, value -> format(value, 0) + "b"));
        rightBottom += 24;
        addPageWidget(new IntSlider(right, rightBottom, columnWidth, "Threat Scan Interval", 2, 40, workingCopy.threatRadar.scanInterval, value -> workingCopy.threatRadar.scanInterval = value, value -> value + "t"));
        rightBottom += 24;
        addPageWidget(new DoubleSlider(right, rightBottom, columnWidth, "Blindside Threshold", -0.95, 0.95, workingCopy.threatRadar.blindsideThreshold, value -> workingCopy.threatRadar.blindsideThreshold = value, value -> format(value, 2)));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Glow Nearby Players Too", workingCopy.threatRadar.highlightPlayers, value -> workingCopy.threatRadar.highlightPlayers = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Player ESP Auto-Enable", workingCopy.playerEsp.autoEnable, value -> workingCopy.playerEsp.autoEnable = value));
        int predictLeftY = Math.max(leftBottom, rightBottom) + 10;
        int predictRightY = predictLeftY;

        addPageWidget(toggle(left, predictLeftY, "Projectile Predict Auto-Enable", workingCopy.projectilePredict.autoEnable, value -> workingCopy.projectilePredict.autoEnable = value));
        predictLeftY += 24;
        addPageWidget(new IntSlider(left, predictLeftY, columnWidth, "Predict Steps", 30, 240, workingCopy.projectilePredict.maxSteps, value -> workingCopy.projectilePredict.maxSteps = value, value -> Integer.toString(value)));

        addPageWidget(new DoubleSlider(right, predictRightY, columnWidth, "Predict Line Width", 1.0, 6.0, workingCopy.projectilePredict.lineWidth, value -> workingCopy.projectilePredict.lineWidth = value, value -> format(value, 1)));
        predictRightY += 24;
        addPageWidget(toggle(right, predictRightY, "Show Landing Marker", workingCopy.projectilePredict.showLandingMarker, value -> workingCopy.projectilePredict.showLandingMarker = value));

        return Math.max(predictLeftY, predictRightY) + 24;
    }

    private int addAccessPage(int left, int y) {
        if (compactLayout()) {
            int width = contentWidth();
            int bottom = y;

            addPageWidget(toggle(left, bottom, "HallowInv Auto-Enable", workingCopy.creativeAccess.autoEnable, value -> workingCopy.creativeAccess.autoEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Open HallowInv On Enable", workingCopy.creativeAccess.openOnEnable, value -> workingCopy.creativeAccess.openOnEnable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Chest Stealer Auto-Enable", workingCopy.chestStealer.autoEnable, value -> workingCopy.chestStealer.autoEnable = value));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Saved Camera Limit", 1, 64, workingCopy.camera.maxSavedPoints, value -> workingCopy.camera.maxSavedPoints = value, value -> Integer.toString(value)));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Show Minimap On Join", workingCopy.minimap.startEnabled, value -> workingCopy.minimap.startEnabled = value));
            bottom += 24;
            addPageWidget(new IntSlider(left, bottom, width, "Minimap Size", 96, 196, workingCopy.minimap.size, value -> workingCopy.minimap.size = value, value -> value + "px"));
            bottom += 24;
            addPageWidget(new DoubleSlider(left, bottom, width, "Minimap Radius", 32.0, 192.0, workingCopy.minimap.range, value -> workingCopy.minimap.range = value, value -> format(value, 0) + "b"));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Show Terrain", workingCopy.minimap.showTerrain, value -> workingCopy.minimap.showTerrain = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Show Player Markers", workingCopy.minimap.showPlayers, value -> workingCopy.minimap.showPlayers = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Persist Anchor Between Sessions", workingCopy.anchorPulse.persistAnchorBetweenSessions, value -> workingCopy.anchorPulse.persistAnchorBetweenSessions = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Show Anchor Coordinates", workingCopy.anchorPulse.showExactCoordinates, value -> workingCopy.anchorPulse.showExactCoordinates = value));

            return bottom + 24;
        }

        int right = rightColumnLeft();
        int columnWidth = columnWidth();
        int leftBottom = y;
        int rightBottom = y;

        addPageWidget(toggle(left, leftBottom, "HallowInv Auto-Enable", workingCopy.creativeAccess.autoEnable, value -> workingCopy.creativeAccess.autoEnable = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Open HallowInv On Enable", workingCopy.creativeAccess.openOnEnable, value -> workingCopy.creativeAccess.openOnEnable = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Chest Stealer Auto-Enable", workingCopy.chestStealer.autoEnable, value -> workingCopy.chestStealer.autoEnable = value));
        leftBottom += 24;
        addPageWidget(new IntSlider(left, leftBottom, columnWidth, "Saved Camera Limit", 1, 64, workingCopy.camera.maxSavedPoints, value -> workingCopy.camera.maxSavedPoints = value, value -> Integer.toString(value)));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Show Minimap On Join", workingCopy.minimap.startEnabled, value -> workingCopy.minimap.startEnabled = value));
        leftBottom += 24;
        addPageWidget(new IntSlider(left, leftBottom, columnWidth, "Minimap Size", 96, 196, workingCopy.minimap.size, value -> workingCopy.minimap.size = value, value -> value + "px"));
        leftBottom += 24;
        addPageWidget(new DoubleSlider(left, leftBottom, columnWidth, "Minimap Radius", 32.0, 192.0, workingCopy.minimap.range, value -> workingCopy.minimap.range = value, value -> format(value, 0) + "b"));

        addPageWidget(toggle(right, rightBottom, "Persist Anchor Between Sessions", workingCopy.anchorPulse.persistAnchorBetweenSessions, value -> workingCopy.anchorPulse.persistAnchorBetweenSessions = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Show Anchor Coordinates", workingCopy.anchorPulse.showExactCoordinates, value -> workingCopy.anchorPulse.showExactCoordinates = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Show Terrain", workingCopy.minimap.showTerrain, value -> workingCopy.minimap.showTerrain = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Show Player Markers", workingCopy.minimap.showPlayers, value -> workingCopy.minimap.showPlayers = value));

        return Math.max(leftBottom, rightBottom) + 24;
    }

    private int addProtectionPage(int left, int y) {
        if (compactLayout()) {
            int width = contentWidth();
            int bottom = y;

            addPageWidget(toggle(left, bottom, "Invulnerable", workingCopy.protection.invulnerable, value -> workingCopy.protection.invulnerable = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Block Drowning Damage", workingCopy.protection.blockDrowningDamage, value -> workingCopy.protection.blockDrowningDamage = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Block Fall Damage", workingCopy.protection.blockFallDamage, value -> workingCopy.protection.blockFallDamage = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Block Freeze Damage", workingCopy.protection.blockFreezeDamage, value -> workingCopy.protection.blockFreezeDamage = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Block Fire Damage", workingCopy.protection.blockFireDamage, value -> workingCopy.protection.blockFireDamage = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Always Keep Inventory", workingCopy.protection.keepInventory, value -> workingCopy.protection.keepInventory = value));
            bottom += 24;
            addPageWidget(toggle(left, bottom, "Block PvP Damage", workingCopy.protection.blockPvpDamage, value -> workingCopy.protection.blockPvpDamage = value));

            return bottom + 24;
        }

        int right = rightColumnLeft();
        int columnWidth = columnWidth();
        int leftBottom = y;
        int rightBottom = y;

        addPageWidget(toggle(left, leftBottom, "Invulnerable", workingCopy.protection.invulnerable, value -> workingCopy.protection.invulnerable = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Block Drowning Damage", workingCopy.protection.blockDrowningDamage, value -> workingCopy.protection.blockDrowningDamage = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Block Fall Damage", workingCopy.protection.blockFallDamage, value -> workingCopy.protection.blockFallDamage = value));
        leftBottom += 24;
        addPageWidget(toggle(left, leftBottom, "Block Freeze Damage", workingCopy.protection.blockFreezeDamage, value -> workingCopy.protection.blockFreezeDamage = value));

        addPageWidget(toggle(right, rightBottom, "Block Fire Damage", workingCopy.protection.blockFireDamage, value -> workingCopy.protection.blockFireDamage = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Always Keep Inventory", workingCopy.protection.keepInventory, value -> workingCopy.protection.keepInventory = value));
        rightBottom += 24;
        addPageWidget(toggle(right, rightBottom, "Block PvP Damage", workingCopy.protection.blockPvpDamage, value -> workingCopy.protection.blockPvpDamage = value));

        return Math.max(leftBottom, rightBottom) + 24;
    }

    private CycleButton<Boolean> toggle(int x, int y, String label, boolean current, java.util.function.Consumer<Boolean> setter) {
        return CycleButton.onOffBuilder(current)
            .create(x, y, columnWidth(), 20, Component.literal(label), (button, value) -> setter.accept(value));
    }

    private void saveAndClose() {
        if (xRayBlocksBox != null) {
            workingCopy.xray.trackedBlocks = parseBlockList(xRayBlocksBox.getValue());
        }
        if (xRayResourcePackBox != null) {
            workingCopy.xray.resourcePackPath = normalizeText(xRayResourcePackBox.getValue());
        }
        HallowConfigManager.applyAndSave(workingCopy);
        HallowRuntimeState.onConfigSaved(this.minecraft);
        onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(previous);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelLeft = panelLeft();
        int panelRight = panelRight();
        int panelTop = panelTop();
        int panelBottom = panelBottom();

        graphics.fill(0, 0, this.width, this.height, 0xD3090C12);
        graphics.fill(0, 0, this.width, this.height / 3, 0x2B1A2635);
        graphics.fill(panelLeft - 6, panelTop - 6, panelRight + 6, panelBottom + 6, 0x33000000);
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xDA131A23);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 3, 0xFFCB9344);
        graphics.fill(panelLeft + 1, panelTop + 4, panelRight - 1, panelTop + 28, 0x4A243242);
        graphics.fill(contentLeft(), contentTop(), panelRight - 10, contentBottom(), 0x54101920);
        drawContentLanes(graphics);
        graphics.fill(contentLeft(), helperCardTop(), panelRight - 10, footerTop() - 8, 0x74121922);

        graphics.drawString(this.font, this.title, panelLeft + 12, panelTop + 10, 0xFFF4DFC0, true);
        graphics.drawString(this.font, category.subtitle(), panelLeft + 12, panelTop + 22, 0xFF9BAABB, false);
        int chipX = panelRight - 12;
        chipX = drawChip(graphics, chipX, panelTop + 10, pageWidgets.size() + " controls", 0xFF5A8AC9);
        chipX = drawChip(graphics, chipX - 6, panelTop + 10, category.label, 0xFFCB9344);
        if (maxScroll > 0) {
            drawChip(graphics, chipX - 6, panelTop + 10, "Scroll", 0xFF7E95E8);
        }

        if (category == Category.VISION && xRayBlocksBox != null && xRayBlocksBox.visible) {
            graphics.drawString(this.font, "Tracked X-Ray blocks (comma-separated ids; blank resets to ore defaults)", xRayBlocksBox.getX(), xRayBlocksBox.getY() - 11, 0xFF8FA0B8, false);
            graphics.drawString(this.font, "Current list resolves to " + parseBlockList(xRayBlocksBox.getValue()).size() + " block ids", xRayBlocksBox.getX(), xRayBlocksBox.getY() + 25, 0xFF8FA0B8, false);
            if (xRayResourcePackBox != null && xRayResourcePackBox.visible) {
                graphics.drawString(this.font, "Managed X-Ray pack source (copied into resourcepacks when X-Ray is enabled)", xRayResourcePackBox.getX(), xRayResourcePackBox.getY() - 11, 0xFF8FA0B8, false);
                graphics.drawString(this.font, xRayPackSourceStatus(xRayResourcePackBox.getValue()), xRayResourcePackBox.getX(), xRayResourcePackBox.getY() + 25, 0xFF8FA0B8, false);
            }
        }

        if (maxScroll > 0) {
            graphics.drawString(this.font, Component.literal(scrollOffset + " / " + maxScroll), panelRight - 42, contentTop() + 6, 0xFF8FA0B8, false);
        }

        graphics.drawString(this.font, categoryHelperText(), contentLeft() + 10, helperTextY(), 0xFFDDE5EE, false);
        graphics.drawString(this.font, "Config file: " + HallowStorage.configPath().getFileName() + " | F7 deck shortcuts: F6 layer, V HallowInv, H/J target copy.", contentLeft() + 10, helperTextY() + 12, 0xFF90A1B3, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void updateScrollBounds() {
        int viewportHeight = Math.max(0, contentBottom() - contentTop() - 8);
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private void applyScroll() {
        int viewportTop = contentTop() + 4;
        int viewportBottom = contentBottom() - 24;

        for (LayoutWidget entry : pageWidgets) {
            AbstractWidget widget = entry.widget();
            int adjustedY = entry.baseY() - scrollOffset;
            widget.setY(adjustedY);
            boolean visible = adjustedY + widget.getHeight() >= viewportTop && adjustedY <= viewportBottom;
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private <T extends AbstractWidget> T addPageWidget(T widget) {
        pageWidgets.add(new LayoutWidget(widget, widget.getY()));
        return addRenderableWidget(widget);
    }

    private int panelWidth() {
        int available = Math.max(MIN_PANEL_WIDTH, this.width - 24);
        int maxAllowed = Math.max(MIN_PANEL_WIDTH, this.width - 8);
        return Math.min(BASE_PANEL_WIDTH, Math.min(available, maxAllowed));
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelRight() {
        return panelLeft() + panelWidth();
    }

    private int panelTop() {
        return 14;
    }

    private int panelBottom() {
        return this.height - 10;
    }

    private int contentLeft() {
        return panelLeft() + 10;
    }

    private int contentTop() {
        return panelTop() + 48 + tabAreaHeight();
    }

    private int helperTextY() {
        return helperCardTop() + 8;
    }

    private int helperCardTop() {
        return footerTop() - 42;
    }

    private int footerTop() {
        return panelBottom() - footerHeight() - 8;
    }

    private int contentBottom() {
        return Math.max(contentTop() + 92, helperCardTop() - 10);
    }

    private int columnWidth() {
        return compactLayout()
            ? contentWidth()
            : (panelWidth() - 20 - COLUMN_GAP) / 2;
    }

    private int rightColumnLeft() {
        return compactLayout()
            ? contentLeft()
            : contentLeft() + columnWidth() + COLUMN_GAP;
    }

    private int contentWidth() {
        return panelRight() - contentLeft() - 10;
    }

    private boolean compactLayout() {
        return panelWidth() < 360;
    }

    private boolean stackedFooter() {
        return panelWidth() < 380;
    }

    private int footerHeight() {
        return stackedFooter() ? 72 : 24;
    }

    private int tabsPerRow() {
        return compactLayout() ? 3 : Category.values().length;
    }

    private int tabAreaHeight() {
        int rows = (int) Math.ceil(Category.values().length / (double) tabsPerRow());
        return (rows * 20) + ((rows - 1) * TAB_GAP);
    }

    private int tabWidth(int rowItemCount) {
        int available = panelWidth() - 16 - ((rowItemCount - 1) * TAB_GAP);
        return available / rowItemCount;
    }

    private boolean isInsideContent(double mouseX, double mouseY) {
        return mouseX >= contentLeft()
            && mouseX <= panelRight() - 10
            && mouseY >= contentTop()
            && mouseY <= contentBottom();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> parseBlockList(String value) {
        LinkedHashSet<String> blockIds = new LinkedHashSet<>();
        if (value != null) {
            for (String part : value.split(",")) {
                String normalized = HallowConfig.normalizeBlockId(part);
                if (!normalized.isEmpty()) {
                    blockIds.add(normalized);
                }
            }
        }

        if (blockIds.isEmpty()) {
            return HallowConfig.defaultXRayBlockIds();
        }

        return new ArrayList<>(blockIds);
    }

    private static String format(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private void drawContentLanes(GuiGraphics graphics) {
        int laneTop = contentTop() + 4;
        int laneBottom = contentBottom() - 8;
        if (compactLayout()) {
            graphics.fill(contentLeft(), laneTop, panelRight() - 10, laneBottom, 0x4617202A);
            return;
        }

        graphics.fill(contentLeft(), laneTop, contentLeft() + columnWidth(), laneBottom, 0x4017202A);
        graphics.fill(rightColumnLeft(), laneTop, rightColumnLeft() + columnWidth(), laneBottom, 0x40131C25);
    }

    private int drawChip(GuiGraphics graphics, int right, int top, String text, int accentColor) {
        int width = this.font.width(text) + 12;
        int left = right - width;
        graphics.fill(left, top, right, top + 14, accentColor);
        graphics.fill(left + 1, top + 1, right - 1, top + 13, 0xD010151B);
        graphics.drawString(this.font, fitText(text, 110), left + 6, top + 3, 0xFFF4E4C1, false);
        return left;
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int targetWidth = Math.max(0, maxWidth - this.font.width(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && this.font.width(trimmed) > targetWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private String categoryHelperText() {
        return switch (category) {
            case VISION -> "Vision settings cover fullbright, bundled X-Ray packs, ore targets, and NoRender cleanup.";
            case TRAVERSAL -> "Traversal tunes movement assists, speed settings, and friction or collision modifiers.";
            case AWARENESS -> "Awareness controls radar range, prediction detail, and nearby tracking behavior.";
            case ACCESS_AND_HUD -> "Access/HUD handles HallowInv defaults, minimap presentation, saved cameras, and anchor persistence.";
            case PROTECTION -> "Protection values sync to singleplayer and Hallow-enabled servers when support exists.";
        };
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String xRayPackSourceStatus(String value) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            return "No override path configured. Hallow will use the built-in X-Ray pack.";
        }

        try {
            Path path = Path.of(normalized);
            return Files.exists(path)
                ? "Override pack found: " + path.getFileName()
                : "Override pack missing. Hallow will use the built-in X-Ray pack.";
        } catch (InvalidPathException ignored) {
            return "Invalid override path. Hallow will use the built-in X-Ray pack.";
        }
    }

    private enum Category {
        VISION("Vision", "Vision stack: Fullbright, X-Ray pack control, and NoRender"),
        TRAVERSAL("Traversal", "Movement cheats and mobility tuning"),
        AWARENESS("Awareness", "Radar, trajectory previews, and nearby tracking"),
        ACCESS_AND_HUD("Access/HUD", "HallowInv, camera points, anchors, minimap, and the F7 menu"),
        PROTECTION("Protection", "Damage immunity, keep-inventory, and PvP shields");

        private final String label;
        private final String subtitle;

        Category(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }

        private Component labelComponent() {
            return Component.literal(label);
        }

        private String subtitle() {
            return subtitle;
        }
    }

    private record LayoutWidget(AbstractWidget widget, int baseY) {
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private final Function<Integer, String> formatter;

        private IntSlider(int x, int y, int width, String label, int min, int max, int initialValue, IntConsumer setter, Function<Integer, String> formatter) {
            super(x, y, width, ROW_HEIGHT, Component.empty(), normalize(initialValue, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + formatter.apply(currentValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
            updateMessage();
        }

        private int currentValue() {
            return min + (int) Math.round(value * (max - min));
        }

        private static double normalize(int value, int min, int max) {
            return (value - min) / (double) (max - min);
        }
    }

    private static final class DoubleSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;
        private final Function<Double, String> formatter;

        private DoubleSlider(int x, int y, int width, String label, double min, double max, double initialValue, DoubleConsumer setter, Function<Double, String> formatter) {
            super(x, y, width, ROW_HEIGHT, Component.empty(), normalize(initialValue, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + formatter.apply(currentValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
            updateMessage();
        }

        private double currentValue() {
            return min + (value * (max - min));
        }

        private static double normalize(double value, double min, double max) {
            return (value - min) / (max - min);
        }
    }
}
