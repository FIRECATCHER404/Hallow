package com.hallow.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HallowHudRenderer {
    private static final int PANEL_GAP = 8;
    private static final int PANEL_PADDING = 8;
    private static final int TITLE_GAP = 6;
    private static final int MIN_COLUMN_WIDTH = 118;
    private static final int MIN_TEXT_WIDTH = 72;
    private static final int MAX_COLUMNS = 5;

    public LayoutResult measure(Font font, List<Section> sections, int viewportWidth) {
        if (font == null || sections.isEmpty() || viewportWidth <= 0) {
            return new LayoutResult(0);
        }

        return new LayoutResult(chooseLayout(font, sections, viewportWidth).contentHeight());
    }

    public LayoutResult renderViewport(
        GuiGraphics graphics,
        Font font,
        List<Section> sections,
        int viewportLeft,
        int viewportTop,
        int viewportWidth,
        int viewportHeight,
        int scrollOffset
    ) {
        if (font == null || sections.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) {
            return new LayoutResult(0);
        }

        Layout layout = chooseLayout(font, sections, viewportWidth);
        int viewportBottom = viewportTop + viewportHeight;

        for (PanelPlacement placement : layout.placements()) {
            int top = viewportTop + placement.y() - scrollOffset;
            int bottom = top + placement.section().height();
            if (bottom < viewportTop || top > viewportBottom) {
                continue;
            }

            renderPanel(graphics, font, placement.section(), viewportLeft + placement.x(), top);
        }

        return new LayoutResult(layout.contentHeight());
    }

    private Layout chooseLayout(Font font, List<Section> sections, int viewportWidth) {
        int maxColumns = Math.max(1, Math.min(MAX_COLUMNS, Math.max(1, (viewportWidth + PANEL_GAP) / (MIN_COLUMN_WIDTH + PANEL_GAP))));
        Layout best = null;

        for (int columns = 1; columns <= maxColumns; columns++) {
            int columnWidth = Math.max(72, (viewportWidth - ((columns - 1) * PANEL_GAP)) / columns);
            int textWidth = Math.max(MIN_TEXT_WIDTH, columnWidth - (PANEL_PADDING * 2));
            List<WrappedSection> wrappedSections = wrapSections(font, sections, textWidth, columnWidth);
            Layout candidate = distribute(columnWidth, wrappedSections, columns);
            if (best == null
                || candidate.contentHeight() < best.contentHeight()
                || (candidate.contentHeight() == best.contentHeight() && candidate.columns() > best.columns())) {
                best = candidate;
            }
        }

        return best;
    }

    private Layout distribute(int columnWidth, List<WrappedSection> wrappedSections, int columns) {
        int[] columnHeights = new int[columns];
        List<PanelPlacement> placements = new ArrayList<>();

        for (WrappedSection section : wrappedSections) {
            int column = shortestColumn(columnHeights);
            int x = column * (columnWidth + PANEL_GAP);
            int y = columnHeights[column];
            placements.add(new PanelPlacement(section, x, y));
            columnHeights[column] += section.height() + PANEL_GAP;
        }

        int contentHeight = 0;
        for (int height : columnHeights) {
            if (height > 0) {
                contentHeight = Math.max(contentHeight, height - PANEL_GAP);
            }
        }

        return new Layout(placements, contentHeight, columns);
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

    private void renderPanel(GuiGraphics graphics, Font font, WrappedSection section, int left, int top) {
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

    public record Section(String title, int accentColor, List<String> lines) {
        public Section {
            lines = List.copyOf(lines);
        }
    }

    public record LayoutResult(int contentHeight) {
    }

    private record WrappedSection(String title, int accentColor, List<String> lines, int width, int height) {
    }

    private record PanelPlacement(WrappedSection section, int x, int y) {
    }

    private record Layout(List<PanelPlacement> placements, int contentHeight, int columns) {
    }
}
