package com.hallow.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HallowHudRenderer {
    private static final int PANEL_GAP = 12;
    private static final int PANEL_PADDING = 10;
    private static final int HEADER_GAP = 8;
    private static final int TITLE_GAP = 4;
    private static final int ENTRY_GAP = 8;
    private static final int ENTRY_PADDING_X = 8;
    private static final int ENTRY_PADDING_Y = 6;
    private static final int MIN_COLUMN_WIDTH = 156;
    private static final int MIN_TEXT_WIDTH = 92;
    private static final int BADGE_RESERVE = 56;
    private static final int MAX_COLUMNS = 4;

    public LayoutResult measure(Font font, List<Section> sections, int viewportWidth) {
        if (font == null || sections.isEmpty() || viewportWidth <= 0) {
            return new LayoutResult(0);
        }

        return new LayoutResult(chooseLayout(font, sections, viewportWidth).contentHeight());
    }

    public RenderResult renderViewport(
        GuiGraphics graphics,
        Font font,
        List<Section> sections,
        int viewportLeft,
        int viewportTop,
        int viewportWidth,
        int viewportHeight,
        int scrollOffset,
        int mouseX,
        int mouseY
    ) {
        if (font == null || sections.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) {
            return new RenderResult(0, List.of());
        }

        Layout layout = chooseLayout(font, sections, viewportWidth);
        int viewportBottom = viewportTop + viewportHeight;
        List<ClickTarget> clickTargets = new ArrayList<>();

        for (PanelPlacement placement : layout.placements()) {
            int top = viewportTop + placement.y() - scrollOffset;
            int bottom = top + placement.section().height();
            if (bottom < viewportTop || top > viewportBottom) {
                continue;
            }

            renderPanel(graphics, font, placement.section(), viewportLeft + placement.x(), top, mouseX, mouseY, clickTargets);
        }

        return new RenderResult(layout.contentHeight(), List.copyOf(clickTargets));
    }

    private Layout chooseLayout(Font font, List<Section> sections, int viewportWidth) {
        int maxColumns = Math.max(1, Math.min(MAX_COLUMNS, Math.max(1, (viewportWidth + PANEL_GAP) / (MIN_COLUMN_WIDTH + PANEL_GAP))));
        Layout best = null;

        for (int columns = 1; columns <= maxColumns; columns++) {
            int columnWidth = Math.max(92, (viewportWidth - ((columns - 1) * PANEL_GAP)) / columns);
            int textWidth = Math.max(MIN_TEXT_WIDTH, columnWidth - (PANEL_PADDING * 2) - (ENTRY_PADDING_X * 2) - BADGE_RESERVE);
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
        int titleLineStep = font.lineHeight + 2;
        int detailLineStep = font.lineHeight + 1;

        for (Section section : sections) {
            List<String> subtitleLines = wrapLine(font, section.subtitle(), columnWidth - (PANEL_PADDING * 2));
            List<WrappedEntry> entries = new ArrayList<>(section.entries().size());
            for (Entry entry : section.entries()) {
                List<String> titleLines = wrapLine(font, entry.label(), textWidth);
                if (titleLines.isEmpty()) {
                    titleLines = List.of("-");
                }

                List<String> detailLines = wrapLine(font, entry.detail(), textWidth);
                int height = (ENTRY_PADDING_Y * 2) + (titleLines.size() * titleLineStep);
                if (!detailLines.isEmpty()) {
                    height += 3 + (detailLines.size() * detailLineStep);
                }
                entries.add(new WrappedEntry(entry, titleLines, detailLines, height));
            }

            if (entries.isEmpty()) {
                entries.add(new WrappedEntry(new Entry("-", "", "", () -> {
                }, 0xFF4B5A6C, false), List.of("-"), List.of(), (ENTRY_PADDING_Y * 2) + titleLineStep));
            }

            int headerHeight = font.lineHeight;
            if (!subtitleLines.isEmpty()) {
                headerHeight += TITLE_GAP + (subtitleLines.size() * detailLineStep);
            }

            int entriesHeight = 0;
            for (int index = 0; index < entries.size(); index++) {
                entriesHeight += entries.get(index).height();
                if (index + 1 < entries.size()) {
                    entriesHeight += ENTRY_GAP;
                }
            }

            int height = (PANEL_PADDING * 2) + headerHeight + HEADER_GAP + entriesHeight;
            wrapped.add(new WrappedSection(section.title(), subtitleLines, section.accentColor(), entries, columnWidth, height));
        }
        return wrapped;
    }

    private List<String> wrapLine(Font font, String line, int maxWidth) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

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

    private void renderPanel(
        GuiGraphics graphics,
        Font font,
        WrappedSection section,
        int left,
        int top,
        int mouseX,
        int mouseY,
        List<ClickTarget> clickTargets
    ) {
        int right = left + section.width();
        int bottom = top + section.height();
        int titleLineStep = font.lineHeight + 2;
        int detailLineStep = font.lineHeight + 1;

        graphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0x33000000);
        graphics.fill(left, top, right, bottom, 0xDD111822);
        graphics.fill(left, top, right, top + 3, section.accentColor());
        graphics.fill(left + 1, top + 4, right - 1, bottom - 1, 0xA718202B);
        graphics.fill(left + 1, top + 1, right - 1, top + 16, 0x222C3A4A);

        int titleY = top + PANEL_PADDING;
        graphics.drawString(font, section.title(), left + PANEL_PADDING, titleY, 0xFFF8E8C0, true);
        drawBadge(graphics, font, Integer.toString(section.entries().size()), right - PANEL_PADDING, titleY - 2, section.accentColor(), 0xFF0F141B, false);

        int y = titleY + font.lineHeight;
        for (String subtitleLine : section.subtitleLines()) {
            y += TITLE_GAP;
            graphics.drawString(font, subtitleLine, left + PANEL_PADDING, y, 0xFF92A3B8, false);
            y += detailLineStep - TITLE_GAP;
        }
        y += HEADER_GAP;

        int entryLeft = left + PANEL_PADDING;
        int entryRight = right - PANEL_PADDING;

        for (WrappedEntry entry : section.entries()) {
            int entryBottom = y + entry.height();
            boolean hovered = mouseX >= entryLeft && mouseX <= entryRight && mouseY >= y && mouseY <= entryBottom;
            int background = entry.entry().active()
                ? (hovered ? 0xCC253A4B : 0xB8213141)
                : (hovered ? 0xCC222D3A : 0xA619222D);

            graphics.fill(entryLeft, y, entryRight, entryBottom, background);
            graphics.fill(entryLeft, y, entryLeft + 3, entryBottom, entry.entry().accentColor());
            graphics.fill(entryLeft + 4, y, entryRight, y + 1, 0x443A4755);

            int textLeft = entryLeft + ENTRY_PADDING_X;
            int textTop = y + ENTRY_PADDING_Y;
            for (String titleLine : entry.titleLines()) {
                graphics.drawString(font, titleLine, textLeft, textTop, 0xFFFFFFFF, false);
                textTop += titleLineStep;
            }

            if (!entry.detailLines().isEmpty()) {
                textTop += 1;
                for (String detailLine : entry.detailLines()) {
                    graphics.drawString(font, detailLine, textLeft, textTop, 0xFF9AA9BB, false);
                    textTop += detailLineStep;
                }
            }

            if (!entry.entry().badge().isBlank()) {
                int badgeColor = entry.entry().active() ? entry.entry().accentColor() : dimAccent(entry.entry().accentColor());
                drawBadge(graphics, font, entry.entry().badge(), entryRight - ENTRY_PADDING_X, y + ENTRY_PADDING_Y - 1, badgeColor, 0xFF0F141B, hovered);
            }

            clickTargets.add(new ClickTarget(entryLeft, y, entryRight, entryBottom, entry.entry().action()));
            y = entryBottom + ENTRY_GAP;
        }
    }

    private void drawBadge(GuiGraphics graphics, Font font, String text, int right, int top, int accentColor, int textColor, boolean hovered) {
        int width = font.width(text) + 12;
        int left = right - width;
        int bottom = top + 12;
        int background = hovered ? brighten(accentColor, 1.15F) : accentColor;
        graphics.fill(left, top, right, bottom, background);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xCC10161E);
        graphics.drawCenteredString(font, text, left + (width / 2), top + 2, textColor);
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

    private static int dimAccent(int accentColor) {
        return brighten(accentColor, 0.72F);
    }

    private static int brighten(int color, float factor) {
        int alpha = color >>> 24;
        int red = Math.max(36, Math.min(255, (int) (((color >>> 16) & 0xFF) * factor)));
        int green = Math.max(36, Math.min(255, (int) (((color >>> 8) & 0xFF) * factor)));
        int blue = Math.max(36, Math.min(255, (int) ((color & 0xFF) * factor)));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public record Entry(String label, String detail, String badge, Runnable action, int accentColor, boolean active) {
        public Entry {
            label = label == null ? "" : label;
            detail = detail == null ? "" : detail;
            badge = badge == null ? "" : badge;
        }

        public Entry(String label, Runnable action, int accentColor) {
            this(label, "", "", action, accentColor, false);
        }
    }

    public record Section(String title, String subtitle, int accentColor, List<Entry> entries) {
        public Section {
            title = title == null ? "" : title;
            subtitle = subtitle == null ? "" : subtitle;
            entries = List.copyOf(entries);
        }

        public Section(String title, int accentColor, List<Entry> entries) {
            this(title, "", accentColor, entries);
        }
    }

    public record LayoutResult(int contentHeight) {
    }

    public record RenderResult(int contentHeight, List<ClickTarget> clickTargets) {
    }

    public record ClickTarget(int left, int top, int right, int bottom, Runnable action) {
        public boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }

    private record WrappedEntry(Entry entry, List<String> titleLines, List<String> detailLines, int height) {
    }

    private record WrappedSection(String title, List<String> subtitleLines, int accentColor, List<WrappedEntry> entries, int width, int height) {
    }

    private record PanelPlacement(WrappedSection section, int x, int y) {
    }

    private record Layout(List<PanelPlacement> placements, int contentHeight, int columns) {
    }
}
