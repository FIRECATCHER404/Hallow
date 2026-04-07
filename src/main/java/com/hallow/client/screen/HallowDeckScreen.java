package com.hallow.client.screen;

import java.util.List;

import com.hallow.client.HallowClient;
import com.hallow.client.HallowHudRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HallowDeckScreen extends Screen {
    private static final int SCREEN_MARGIN = 10;
    private static final int PANEL_PADDING = 10;
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 22;
    private static final int SCROLL_STEP = 28;

    private final HallowClient owner;
    private final HallowHudRenderer renderer = new HallowHudRenderer();

    private List<HallowHudRenderer.ClickTarget> clickTargets = List.of();
    private int scrollOffset;
    private int maxScroll;

    public HallowDeckScreen(HallowClient owner) {
        super(Component.literal("Hallow Menu"));
        this.owner = owner;
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
        List<HallowHudRenderer.Section> sections = owner.buildDeckSections(this.minecraft);
        int frameLeft = SCREEN_MARGIN;
        int frameTop = SCREEN_MARGIN;
        int frameRight = this.width - SCREEN_MARGIN;
        int frameBottom = this.height - SCREEN_MARGIN;

        int viewportLeft = frameLeft + PANEL_PADDING;
        int viewportTop = frameTop + HEADER_HEIGHT;
        int viewportRight = frameRight - PANEL_PADDING - 6;
        int viewportBottom = frameBottom - FOOTER_HEIGHT - PANEL_PADDING;
        int viewportWidth = Math.max(64, viewportRight - viewportLeft);
        int viewportHeight = Math.max(64, viewportBottom - viewportTop);

        maxScroll = Math.max(0, renderer.measure(this.font, sections, viewportWidth).contentHeight() - viewportHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        graphics.fill(0, 0, this.width, this.height, 0xD0090C12);
        graphics.fill(frameLeft - 4, frameTop - 4, frameRight + 4, frameBottom + 4, 0x44000000);
        graphics.fill(frameLeft, frameTop, frameRight, frameBottom, 0xE0141922);
        graphics.fill(frameLeft, frameTop, frameRight, frameTop + 3, 0xFFCB9344);
        graphics.fill(viewportLeft - 4, viewportTop - 4, viewportRight + 4, viewportBottom + 4, 0x55000000);
        graphics.fill(viewportLeft, viewportTop, viewportRight, viewportBottom, 0x7A101824);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, frameTop + 8, 0xFFF4E5BF);
        graphics.drawCenteredString(this.font, Component.literal("Click any row to run it. F7 or ESC closes. Mouse wheel scrolls."), this.width / 2, frameTop + 20, 0xFFB5C0D1);

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

        if (maxScroll > 0) {
            int trackLeft = frameRight - 12;
            int trackTop = viewportTop;
            int trackBottom = viewportBottom;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(18, (int) ((viewportHeight / (double) (viewportHeight + maxScroll)) * trackHeight));
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = trackTop + (thumbTravel == 0 ? 0 : (int) ((scrollOffset / (double) maxScroll) * thumbTravel));

            graphics.fill(trackLeft, trackTop, trackLeft + 3, trackBottom, 0x55394856);
            graphics.fill(trackLeft, thumbTop, trackLeft + 3, thumbTop + thumbHeight, 0xFFCA9746);
            graphics.drawCenteredString(this.font, Component.literal(scrollOffset + " / " + maxScroll), this.width / 2, frameBottom - 18, 0xFF9EABB9);
        } else {
            graphics.drawCenteredString(this.font, Component.literal("Everything fits on-screen."), this.width / 2, frameBottom - 18, 0xFF9EABB9);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
