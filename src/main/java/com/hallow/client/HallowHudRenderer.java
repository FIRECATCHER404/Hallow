package com.hallow.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HallowHudRenderer {
    private static final int SCREEN_MARGIN = 8;
    private static final int PANEL_GAP = 8;
    private static final int PANEL_PADDING = 8;
    private static final int TITLE_GAP = 6;
    private static final int MIN_COLUMN_WIDTH = 148;
    private static final int MIN_TEXT_WIDTH = 96;
    private static final int MAX_COLUMNS = 4;

    public void render(GuiGraphics graphics, Minecraft client, int anchorLeft, int anchorTop, List<Section> sections) {
        if (client == null || client.font == null || sections.isEmpty()) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int left = clamp(anchorLeft, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - MIN_COLUMN_WIDTH - SCREEN_MARGIN));
        int top = clamp(anchorTop, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - 80));
        int availableWidth = Math.max(MIN_COLUMN_WIDTH, screenWidth - left - SCREEN_MARGIN);
        int availableHeight = Math.max(84, screenHeight - top - SCREEN_MARGIN);

        Layout layout = chooseLayout(client.font, sections, left, top, availableWidth, availableHeight);
        for (PanelPlacement placement : layout.placements()) {
            renderPanel(graphics, client.font, placement);
        }
    }

    private Layout chooseLayout(Font font, List<Section> sections, int left, int top, int availableWidth, int availableHeight) {
        int maxColumns = Math.max(1, Math.min(MAX_COLUMNS, (availableWidth + PANEL_GAP) / (MIN_COLUMN_WIDTH + PANEL_GAP)));
        Layout best = null;

        for (int columns = 1; columns <= maxColumns; columns++) {
            int columnWidth = Math.max(MIN_COLUMN_WIDTH, (availableWidth - ((columns - 1) * PANEL_GAP)) / columns);
            int textWidth = Math.max(MIN_TEXT_WIDTH, columnWidth - (PANEL_PADDING * 2));
            List<WrappedSection> wrappedSections = wrapSections(font, sections, textWidth, columnWidth);
            Layout candidate = distribute(left, top, availableHeight, columnWidth, wrappedSections, columns);
            if (best == null
                || candidate.overflow() < best.overflow()
                || (candidate.overflow() == best.overflow() && candidate.maxColumnHeight() < best.maxColumnHeight())
                || (candidate.overflow() == best.overflow() && candidate.maxColumnHeight() == best.maxColumnHeight() && candidate.columns() > best.columns())) {
                best = candidate;
            }
        }

        return best;
    }

    private Layout distribute(int left, int top, int availableHeight, int columnWidth, List<WrappedSection> wrappedSections, int columns) {
        int[] columnHeights = new int[columns];
        List<PanelPlacement> placements = new ArrayList<>();

        for (WrappedSection section : wrappedSections) {
            int column = shortestColumn(columnHeights);
            int x = left + (column * (columnWidth + PANEL_GAP));
            int y = top + columnHeights[column];
            placements.add(new PanelPlacement(section, x, y));
            columnHeights[column] += section.height() + PANEL_GAP;
        }

        int maxColumnHeight = 0;
        for (int height : columnHeights) {
            if (height > 0) {
                maxColumnHeight = Math.max(maxColumnHeight, height - PANEL_GAP);
            }
        }

        int overflow = Math.max(0, maxColumnHeight - availableHeight);
        return new Layout(placements, overflow, maxColumnHeight, columns);
    }

    private List<WrappedSection> wrapSections(Font font, List<Section> sections, int textWidth, int columnWidth) {
        List<WrappedSection> wrapped = new ArrayList<>(sections.size());
        int lineStep = font.lineHeight + 2;
        for (Section section : sections) {
            List<String> lines = new ArrayList<>();
            for (String line : section.lines()) {
                if (line == null || line.isBlank()) {
                    lines.add("");
                    continue;
                }

                lines.addAll(wrapLine(font, line, textWidth));
            }

            if (lines.isEmpty()) {
                lines.add("-");
            }

            int height = (PANEL_PADDING * 2) + font.lineHeight + TITLE_GAP + (lines.size() * lineStep);
            wrapped.add(new WrappedSection(section.title(), section.accentColor(), lines, columnWidth, height));
        }
        return wrapped;
    }

    private List<String> wrapLine(Font font, String line, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        String remaining = line.trim();
        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= maxWidth) {
                wrapped.add(remaining);
                break;
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
            if (segment.isEmpty()) {
                segment = remaining.substring(0, candidateLength).trim();
                split = candidateLength;
            }

            wrapped.add(segment);
            remaining = remaining.substring(split).trim();
        }
        return wrapped;
    }

    private void renderPanel(GuiGraphics graphics, Font font, PanelPlacement placement) {
        WrappedSection section = placement.section();
        int left = placement.x();
        int top = placement.y();
        int right = left + section.width();
        int bottom = top + section.height();
        int lineStep = font.lineHeight + 2;

        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0x44000000);
        graphics.fill(left, top, right, bottom, 0xD0121822);
        graphics.fill(left, top, right, top + 3, section.accentColor());
        graphics.fill(left + 1, top + 4, right - 1, bottom - 1, 0x941A2330);

        int titleY = top + PANEL_PADDING;
        graphics.drawString(font, section.title(), left + PANEL_PADDING, titleY, 0xFFF4E5BF, true);

        int y = titleY + font.lineHeight + TITLE_GAP;
        for (String line : section.lines()) {
            if (!line.isEmpty()) {
                graphics.drawString(font, line, left + PANEL_PADDING, y, 0xFFFFFFFF, false);
            }
            y += lineStep;
        }
    }

    private static int shortestColumn(int[] heights) {
        int bestIndex = 0;
        int bestHeight = heights[0];
        for (int index = 1; index < heights.length; index++) {
            if (heights[index] < bestHeight) {
                bestIndex = index;
                bestHeight = heights[index];
            }
        }
        return bestIndex;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Section(String title, int accentColor, List<String> lines) {
        public Section {
            lines = List.copyOf(lines);
        }
    }

    private record WrappedSection(String title, int accentColor, List<String> lines, int width, int height) {
    }

    private record PanelPlacement(WrappedSection section, int x, int y) {
    }

    private record Layout(List<PanelPlacement> placements, int overflow, int maxColumnHeight, int columns) {
    }
}
