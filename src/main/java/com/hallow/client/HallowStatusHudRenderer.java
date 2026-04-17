package com.hallow.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class HallowStatusHudRenderer {
    private static final int SCREEN_MARGIN = 6;
    private static final int PANEL_PADDING_X = 8;
    private static final int PANEL_PADDING_Y = 6;
    private static final int TITLE_GAP = 6;
    private static final int LINE_GAP = 2;

    public void render(GuiGraphics graphics, Minecraft client, String title, List<String> lines, int requestedLeft, int requestedTop) {
        if (graphics == null || client == null || client.font == null || client.getWindow() == null) {
            return;
        }

        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int availableTextWidth = Math.max(72, screenWidth - (SCREEN_MARGIN * 2) - (PANEL_PADDING_X * 2));
        int preferredTextWidth = Math.max(160, screenWidth / 3);
        int maxTextWidth = Math.min(availableTextWidth, preferredTextWidth);

        List<String> wrappedLines = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                wrapInto(wrappedLines, font, line, maxTextWidth);
            }
        }
        if (wrappedLines.isEmpty()) {
            wrappedLines.add("No active module status.");
        }

        int contentWidth = font.width(title);
        for (String line : wrappedLines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }

        int lineStep = font.lineHeight + LINE_GAP;
        int panelWidth = contentWidth + (PANEL_PADDING_X * 2);
        int panelHeight = (PANEL_PADDING_Y * 2)
            + font.lineHeight
            + TITLE_GAP
            + (wrappedLines.size() * lineStep)
            - LINE_GAP;
        int left = Mth.clamp(requestedLeft, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - panelWidth - SCREEN_MARGIN));
        int top = Mth.clamp(requestedTop, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - panelHeight - SCREEN_MARGIN));
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0x44000000);
        graphics.fill(left, top, right, bottom, 0xCC11161E);
        graphics.fill(left, top, right, top + 2, 0xFFCB9344);
        graphics.fill(left + 1, top + 3, right - 1, bottom - 1, 0x9C171D27);

        int textX = left + PANEL_PADDING_X;
        int y = top + PANEL_PADDING_Y;
        graphics.drawString(font, title, textX, y, 0xFFF7E4BC, true);
        y += font.lineHeight + TITLE_GAP;

        for (String line : wrappedLines) {
            graphics.drawString(font, line, textX, y, 0xFFE1E8F0, false);
            y += lineStep;
        }
    }

    private void wrapInto(List<String> destination, Font font, String text, int maxWidth) {
        String remaining = text == null ? "" : text.trim();
        if (remaining.isEmpty()) {
            return;
        }

        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= maxWidth) {
                destination.add(remaining);
                return;
            }

            int candidateLength = 1;
            while (candidateLength < remaining.length() && font.width(remaining.substring(0, candidateLength + 1)) <= maxWidth) {
                candidateLength++;
            }

            int split = candidateLength;
            while (split > 1 && remaining.charAt(split - 1) != ' ') {
                split--;
            }
            if (split <= 1) {
                split = candidateLength;
            }

            String segment = remaining.substring(0, split).trim();
            if (!segment.isEmpty()) {
                destination.add(segment);
            }
            remaining = remaining.substring(split).trim();
        }
    }
}
