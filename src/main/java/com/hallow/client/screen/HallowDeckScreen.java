package com.hallow.client.screen;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.hallow.client.HallowCameraController;
import com.hallow.client.HallowClient;
import com.hallow.client.HallowHudRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HallowDeckScreen extends Screen {
    private static final int SCREEN_MARGIN = 12;
    private static final int PANEL_PADDING = 14;
    private static final int HEADER_HEIGHT = 92;
    private static final int FOOTER_HEIGHT = 34;
    private static final int SCROLL_STEP = 24;
    private static final int SEARCH_WIDTH = 220;

    private final HallowClient owner;
    private final HallowHudRenderer renderer = new HallowHudRenderer();

    private List<HallowHudRenderer.ClickTarget> clickTargets = List.of();
    private EditBox filterBox;
    private String filterQuery = "";
    private int scrollOffset;
    private int maxScroll;

    public HallowDeckScreen(HallowClient owner) {
        super(Component.literal("Hallow Deck"));
        this.owner = owner;
    }

    @Override
    protected void init() {
        int frameLeft = SCREEN_MARGIN;
        int frameRight = this.width - SCREEN_MARGIN;
        int boxWidth = Math.min(SEARCH_WIDTH, Math.max(120, frameRight - frameLeft - 36));
        int boxLeft = frameRight - boxWidth - 18;

        filterBox = new EditBox(this.font, boxLeft, SCREEN_MARGIN + 24, boxWidth, 20, Component.literal("Deck filter"));
        filterBox.setMaxLength(64);
        filterBox.setValue(filterQuery);
        filterBox.setHint(Component.literal("Search modules, actions, shortcuts"));
        filterBox.setResponder(value -> {
            filterQuery = normalize(value);
            scrollOffset = 0;
        });
        addRenderableWidget(filterBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.player == null) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.hasControlDown() && event.key() == GLFW.GLFW_KEY_F) || event.key() == GLFW.GLFW_KEY_SLASH) {
            focusFilterBox();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0 || verticalAmount == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        scrollOffset = clamp(scrollOffset - ((int) Math.signum(verticalAmount) * SCROLL_STEP), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() != 0) {
            return false;
        }

        for (HallowHudRenderer.ClickTarget target : clickTargets) {
            if (target.contains(event.x(), event.y())) {
                target.action().run();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<HallowHudRenderer.Section> sections = filterSections(owner.buildDeckSections(this.minecraft), filterQuery);
        int totalEntries = totalEntries(sections);
        int frameLeft = SCREEN_MARGIN;
        int frameTop = SCREEN_MARGIN;
        int frameRight = this.width - SCREEN_MARGIN;
        int frameBottom = this.height - SCREEN_MARGIN;

        int viewportLeft = frameLeft + PANEL_PADDING;
        int viewportTop = frameTop + HEADER_HEIGHT;
        int viewportRight = frameRight - PANEL_PADDING - 6;
        int viewportBottom = frameBottom - FOOTER_HEIGHT - PANEL_PADDING;
        int viewportWidth = Math.max(96, viewportRight - viewportLeft);
        int viewportHeight = Math.max(64, viewportBottom - viewportTop);

        maxScroll = Math.max(0, renderer.measure(this.font, sections, viewportWidth).contentHeight() - viewportHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        graphics.fill(0, 0, this.width, this.height, 0xE0080B10);
        graphics.fill(0, 0, this.width, this.height / 3, 0x351C2A38);
        graphics.fill(frameLeft - 5, frameTop - 5, frameRight + 5, frameBottom + 5, 0x33000000);
        graphics.fill(frameLeft, frameTop, frameRight, frameBottom, 0xE3131820);
        graphics.fill(frameLeft, frameTop, frameRight, frameTop + 3, 0xFFCD9450);
        graphics.fill(frameLeft + 1, frameTop + 4, frameRight - 1, frameTop + HEADER_HEIGHT - 8, 0xA617202B);
        graphics.fill(viewportLeft - 5, viewportTop - 5, viewportRight + 5, viewportBottom + 5, 0x22000000);
        graphics.fill(viewportLeft, viewportTop, viewportRight, viewportBottom, 0x7A0F1720);
        graphics.fill(viewportLeft + 1, viewportTop + 1, viewportRight - 1, viewportBottom - 1, 0x7A141F2A);
        graphics.fill(frameLeft + 18, frameBottom - FOOTER_HEIGHT - 2, frameRight - 18, frameBottom - 14, 0x80101920);

        graphics.drawString(this.font, this.title, frameLeft + 18, frameTop + 12, 0xFFF7E8C5, true);
        graphics.drawString(this.font, "Fast access to modules, utility actions, and per-profile controls.", frameLeft + 18, frameTop + 26, 0xFF97A8BB, false);
        graphics.drawString(this.font, "Quick Find", frameRight - 18 - (filterBox != null ? filterBox.getWidth() : 0), frameTop + 12, 0xFFCBD6E1, false);

        int chipY = frameTop + 48;
        int chipX = frameLeft + 18;
        int chipRight = frameRight - 18;
        chipX = drawChip(graphics, chipX, chipY, chipRight, "Profile", owner.currentProfileLabel(), 0xFF5A8AC9);
        chipX = drawChip(graphics, chipX, chipY, chipRight, "Active", owner.activeModuleCount() + " modules", 0xFF61B887);
        chipX = drawChip(graphics, chipX, chipY, chipRight, "Results", totalEntries + " controls", 0xFFC18B42);
        String targetName = fallbackName(HallowCameraController.currentTargetName(this.minecraft));
        drawChip(graphics, chipX, chipY, chipRight, "Target", targetName, 0xFF7E95E8);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (sections.isEmpty()) {
            clickTargets = List.of();
            graphics.drawCenteredString(this.font, Component.literal("No deck items match the current filter."), this.width / 2, viewportTop + 24, 0xFFE2CFA1);
            graphics.drawCenteredString(this.font, Component.literal("Try a module name, action, or shortcut key."), this.width / 2, viewportTop + 38, 0xFF9AA8B8);
        } else {
            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            HallowHudRenderer.RenderResult renderResult = renderer.renderViewport(
                graphics,
                this.font,
                sections,
                viewportLeft,
                viewportTop,
                viewportWidth,
                viewportHeight,
                scrollOffset,
                mouseX,
                mouseY
            );
            graphics.disableScissor();
            clickTargets = renderResult.clickTargets();
        }

        if (maxScroll > 0) {
            int trackLeft = frameRight - 13;
            int trackTop = viewportTop;
            int trackBottom = viewportBottom;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(20, (int) ((viewportHeight / (double) (viewportHeight + maxScroll)) * trackHeight));
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = trackTop + (thumbTravel == 0 ? 0 : (int) ((scrollOffset / (double) maxScroll) * thumbTravel));

            graphics.fill(trackLeft, trackTop, trackLeft + 4, trackBottom, 0x443B4755);
            graphics.fill(trackLeft, thumbTop, trackLeft + 4, thumbTop + thumbHeight, 0xFFCB9547);
        }

        String footerLeft = "Click to run. Ctrl+F or / focuses search. F7 or ESC closes.";
        String footerRight = maxScroll > 0 ? "Scroll " + scrollOffset + " / " + maxScroll : "Everything visible";
        graphics.drawString(this.font, footerLeft, frameLeft + 18, frameBottom - 26, 0xFF9EACBB, false);
        graphics.drawString(this.font, footerRight, frameRight - 18 - this.font.width(footerRight), frameBottom - 26, 0xFFD7BA7C, false);
    }

    private void focusFilterBox() {
        if (filterBox == null) {
            return;
        }

        setFocused(filterBox);
        filterBox.setFocused(true);
    }

    private List<HallowHudRenderer.Section> filterSections(List<HallowHudRenderer.Section> sections, String query) {
        if (query.isBlank()) {
            return sections;
        }

        List<HallowHudRenderer.Section> filtered = new ArrayList<>();
        for (HallowHudRenderer.Section section : sections) {
            if (matches(section.title(), query) || matches(section.subtitle(), query)) {
                filtered.add(section);
                continue;
            }

            List<HallowHudRenderer.Entry> entries = section.entries().stream()
                .filter(entry -> matches(entry.label(), query) || matches(entry.detail(), query) || matches(entry.badge(), query))
                .toList();
            if (!entries.isEmpty()) {
                filtered.add(new HallowHudRenderer.Section(section.title(), section.subtitle(), section.accentColor(), entries));
            }
        }
        return filtered;
    }

    private int totalEntries(List<HallowHudRenderer.Section> sections) {
        int total = 0;
        for (HallowHudRenderer.Section section : sections) {
            total += section.entries().size();
        }
        return total;
    }

    private int drawChip(GuiGraphics graphics, int left, int top, int maxRight, String label, String value, int accentColor) {
        String text = label + ": " + fitText(value, 130);
        int width = this.font.width(text) + 14;
        if (left + width > maxRight) {
            return left;
        }

        graphics.fill(left, top, left + width, top + 16, accentColor);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 15, 0xD010161D);
        graphics.drawString(this.font, text, left + 7, top + 4, 0xFFF4E9C9, false);
        return left + width + 6;
    }

    private String fitText(String text, int maxWidth) {
        if (text == null || this.font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }

        String ellipsis = "...";
        int targetWidth = Math.max(0, maxWidth - this.font.width(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && this.font.width(trimmed) > targetWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private String fallbackName(String name) {
        return name == null || name.isBlank() ? "none" : name;
    }

    private boolean matches(String text, String query) {
        return normalize(text).contains(query);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
