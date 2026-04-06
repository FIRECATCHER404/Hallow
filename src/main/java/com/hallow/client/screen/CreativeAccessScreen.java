package com.hallow.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.hallow.client.cheat.modules.CreativeAccessModule;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CreativeAccessScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 252;
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_COLUMNS = 9;
    private static final int ITEM_ROWS = 6;
    private static final int ITEMS_PER_PAGE = ITEM_COLUMNS * ITEM_ROWS;
    private static final int[] DISPLAYED_INVENTORY_SLOTS = createDisplayedSlots();

    private final Screen previous;
    private final CreativeAccessModule module;
    private final List<Item> filteredItems = new ArrayList<>();

    private EditBox searchBox;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button minusButton;
    private Button plusButton;
    private Button oneButton;
    private Button sixteenButton;
    private Button thirtyTwoButton;
    private Button sixtyFourButton;
    private Button maxButton;
    private Button closeButton;

    private Item selectedItem;
    private int selectedCount = 64;
    private int page;

    public CreativeAccessScreen(Screen previous, CreativeAccessModule module) {
        super(Component.literal("HallowInv"));
        this.previous = previous;
        this.module = module;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();

        searchBox = new EditBox(this.font, left + 14, top + 18, 164, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search item or id"));
        searchBox.setResponder(value -> refreshFilter());
        searchBox.setCanLoseFocus(true);
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        previousPageButton = addRenderableWidget(
            Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(left + 184, top + 18, 20, 20)
                .build()
        );
        nextPageButton = addRenderableWidget(
            Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(left + 208, top + 18, 20, 20)
                .build()
        );

        int controlsLeft = left + 248;
        minusButton = addRenderableWidget(
            Button.builder(Component.literal("-"), button -> adjustCount(-1))
                .bounds(controlsLeft, top + 44, 20, 20)
                .build()
        );
        plusButton = addRenderableWidget(
            Button.builder(Component.literal("+"), button -> adjustCount(1))
                .bounds(controlsLeft + 144, top + 44, 20, 20)
                .build()
        );
        oneButton = addRenderableWidget(presetButton(controlsLeft, top + 72, "1", 1));
        sixteenButton = addRenderableWidget(presetButton(controlsLeft + 38, top + 72, "16", 16));
        thirtyTwoButton = addRenderableWidget(presetButton(controlsLeft + 82, top + 72, "32", 32));
        sixtyFourButton = addRenderableWidget(presetButton(controlsLeft, top + 98, "64", 64));
        maxButton = addRenderableWidget(
            Button.builder(Component.literal("Max"), button -> setToMaxStack())
                .bounds(controlsLeft + 62, top + 98, 58, 20)
                .build()
        );
        closeButton = addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 84, top + PANEL_HEIGHT - 28, 70, 20)
                .build()
        );

        refreshFilter();
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.player == null || !module.isEnabled()) {
            onClose();
            return;
        }

        updateButtons();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int itemIndex = itemIndexAt(event.x(), event.y());
        if (itemIndex >= 0) {
            selectedItem = filteredItems.get(itemIndex);
            clampSelectedCount();
            updateButtons();
            return true;
        }

        int displaySlot = inventoryDisplaySlotAt(event.x(), event.y());
        if (displaySlot >= 0) {
            int inventorySlot = DISPLAYED_INVENTORY_SLOTS[displaySlot];
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                module.clearInventorySlot(this.minecraft, inventorySlot);
            } else if (selectedItem != null) {
                module.assignItemToInventory(this.minecraft, selectedItem, selectedCount, inventorySlot);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInItemGrid(mouseX, mouseY) && maxPage() > 0) {
            if (verticalAmount < 0) {
                changePage(1);
            } else if (verticalAmount > 0) {
                changePage(-1);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();

        graphics.fill(0, 0, this.width, this.height, 0xCC090B11);
        graphics.fill(left - 6, top - 6, left + PANEL_WIDTH + 6, top + PANEL_HEIGHT + 6, 0x60000000);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD3151921);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 3, 0xFFCB9344);
        graphics.fill(left + 10, top + 48, left + 228, top + PANEL_HEIGHT - 14, 0x6610161F);
        graphics.fill(left + 238, top + 18, left + PANEL_WIDTH - 14, top + PANEL_HEIGHT - 40, 0x6610161F);

        graphics.drawString(this.font, this.title, left + 14, top + 8, 0xFFF2D8A0, true);
        graphics.drawString(this.font, "Pick an item, then click a slot", left + 120, top + 8, 0xFF9FB0C7, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderPageLabel(graphics, left + 150, top + 44);
        renderItemGrid(graphics, mouseX, mouseY);
        renderSelectionPanel(graphics, left + 248, top + 18);
        renderInventoryGrid(graphics, mouseX, mouseY, left + 248, top + 128);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(previous);
        }
    }

    private void renderPageLabel(GuiGraphics graphics, int x, int y) {
        int currentPage = filteredItems.isEmpty() ? 0 : page + 1;
        int totalPages = filteredItems.isEmpty() ? 0 : maxPage() + 1;
        graphics.drawCenteredString(this.font, Component.literal("Page " + currentPage + " / " + totalPages), x, y, 0xFFD7DEE8);
    }

    private void renderItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft() + 14;
        int top = panelTop() + 52;
        Item hoveredItem = null;

        for (int pageIndex = 0; pageIndex < ITEMS_PER_PAGE; pageIndex++) {
            int absoluteIndex = (page * ITEMS_PER_PAGE) + pageIndex;
            int x = left + ((pageIndex % ITEM_COLUMNS) * SLOT_SIZE);
            int y = top + ((pageIndex / ITEM_COLUMNS) * SLOT_SIZE);

            graphics.fill(x, y, x + 16, y + 16, 0xFF232A36);
            graphics.renderOutline(x - 1, y - 1, 18, 18, 0xAA000000);

            if (absoluteIndex >= filteredItems.size()) {
                continue;
            }

            Item item = filteredItems.get(absoluteIndex);
            ItemStack stack = new ItemStack(item);
            if (item == selectedItem) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFFE2A654);
            } else if (contains(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFF84A6D5);
                hoveredItem = item;
            }

            graphics.renderItem(stack, x, y);
        }

        if (hoveredItem != null) {
            graphics.setTooltipForNextFrame(this.font, new ItemStack(hoveredItem), mouseX, mouseY);
        }
    }

    private void renderSelectionPanel(GuiGraphics graphics, int left, int top) {
        ItemStack preview = selectedItem != null ? new ItemStack(selectedItem, Math.min(selectedCount, maxStackForSelected())) : ItemStack.EMPTY;

        graphics.drawString(this.font, "Selected", left, top + 4, 0xFFF2D8A0, false);
        graphics.fill(left, top + 20, left + 32, top + 52, 0xFF1F2632);
        graphics.renderOutline(left, top + 20, 32, 32, 0xAA000000);

        if (!preview.isEmpty()) {
            graphics.renderItem(preview, left + 8, top + 28);
        }

        String itemName = selectedItem == null ? "No item selected" : preview.getHoverName().getString();
        Identifier itemId = selectedItem == null ? null : BuiltInRegistries.ITEM.getKey(selectedItem);
        graphics.drawString(this.font, itemName, left + 40, top + 24, 0xFFFFFFFF, false);
        graphics.drawString(this.font, itemId == null ? "-" : itemId.toString(), left + 40, top + 38, 0xFF93A4BC, false);
        graphics.drawCenteredString(this.font, Component.literal(Integer.toString(selectedCount)), left + 82, top + 50, 0xFFF7F0E4);
        graphics.drawString(this.font, "Right-click a slot to clear it", left, top + 108, 0xFF91A2B8, false);
    }

    private void renderInventoryGrid(GuiGraphics graphics, int mouseX, int mouseY, int left, int top) {
        Inventory inventory = this.minecraft.player.getInventory();
        int hoveredSlot = -1;

        for (int index = 0; index < DISPLAYED_INVENTORY_SLOTS.length; index++) {
            int x = left + ((index % 9) * SLOT_SIZE);
            int y = top + ((index / 9) * SLOT_SIZE);
            int inventorySlot = DISPLAYED_INVENTORY_SLOTS[index];
            ItemStack stack = inventory.getItem(inventorySlot);

            graphics.fill(x, y, x + 16, y + 16, 0xFF232A36);
            graphics.renderOutline(x - 1, y - 1, 18, 18, 0xAA000000);

            if (inventorySlot < 9 && inventory.getSelectedSlot() == inventorySlot) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFFE2A654);
            } else if (contains(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFF84A6D5);
                hoveredSlot = inventorySlot;
            }

            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(this.font, stack, x, y);
            }
        }

        graphics.drawString(this.font, "Inventory Target", left, top - 12, 0xFFF2D8A0, false);
        if (hoveredSlot >= 0) {
            ItemStack hoveredStack = inventory.getItem(hoveredSlot);
            if (!hoveredStack.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
            }
        }
    }

    private void refreshFilter() {
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        FeatureFlagSet enabledFeatures = this.minecraft != null && this.minecraft.player != null
            ? this.minecraft.player.connection.enabledFeatures()
            : null;

        filteredItems.clear();
        BuiltInRegistries.ITEM.stream()
            .filter(item -> item != Items.AIR)
            .filter(item -> enabledFeatures == null || new ItemStack(item).isItemEnabled(enabledFeatures))
            .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
            .filter(item -> matchesQuery(item, query))
            .forEach(filteredItems::add);

        page = Math.max(0, Math.min(page, maxPage()));
        if (
            selectedItem != null
                && enabledFeatures != null
                && !new ItemStack(selectedItem).isItemEnabled(enabledFeatures)
        ) {
            selectedItem = null;
        }
        clampSelectedCount();
        updateButtons();
    }

    private boolean matchesQuery(Item item, String query) {
        if (query.isBlank()) {
            return true;
        }

        Identifier identifier = BuiltInRegistries.ITEM.getKey(item);
        String idString = identifier != null ? identifier.toString().toLowerCase(Locale.ROOT) : "";
        String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
        return idString.contains(query) || name.contains(query);
    }

    private void adjustCount(int delta) {
        selectedCount = Math.max(1, Math.min(maxStackForSelected(), selectedCount + delta));
        updateButtons();
    }

    private void setToMaxStack() {
        selectedCount = maxStackForSelected();
        updateButtons();
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(maxPage(), page + delta));
        updateButtons();
    }

    private void clampSelectedCount() {
        selectedCount = Math.max(1, Math.min(maxStackForSelected(), selectedCount));
    }

    private void updateButtons() {
        int maxStack = maxStackForSelected();
        previousPageButton.active = page > 0;
        nextPageButton.active = page < maxPage();
        minusButton.active = selectedItem != null && selectedCount > 1;
        plusButton.active = selectedItem != null && selectedCount < maxStack;
        oneButton.active = selectedItem != null && selectedCount != 1;
        sixteenButton.active = selectedItem != null && maxStack >= 16 && selectedCount != 16;
        thirtyTwoButton.active = selectedItem != null && maxStack >= 32 && selectedCount != 32;
        sixtyFourButton.active = selectedItem != null && maxStack >= 64 && selectedCount != 64;
        maxButton.active = selectedItem != null && selectedCount != maxStack;
    }

    private Button presetButton(int x, int y, String label, int amount) {
        return Button.builder(Component.literal(label), button -> {
            selectedCount = Math.min(amount, maxStackForSelected());
            updateButtons();
        }).bounds(x, y, 36, 20).build();
    }

    private int itemIndexAt(double mouseX, double mouseY) {
        if (!isInItemGrid(mouseX, mouseY)) {
            return -1;
        }

        int column = (int) ((mouseX - (panelLeft() + 14)) / SLOT_SIZE);
        int row = (int) ((mouseY - (panelTop() + 52)) / SLOT_SIZE);
        int absoluteIndex = (page * ITEMS_PER_PAGE) + (row * ITEM_COLUMNS) + column;
        return absoluteIndex >= 0 && absoluteIndex < filteredItems.size() ? absoluteIndex : -1;
    }

    private int inventoryDisplaySlotAt(double mouseX, double mouseY) {
        int left = panelLeft() + 248;
        int top = panelTop() + 128;
        if (!contains(mouseX, mouseY, left - 1, top - 1, (9 * SLOT_SIZE), (4 * SLOT_SIZE))) {
            return -1;
        }

        int column = (int) ((mouseX - left) / SLOT_SIZE);
        int row = (int) ((mouseY - top) / SLOT_SIZE);
        int index = (row * 9) + column;
        return index >= 0 && index < DISPLAYED_INVENTORY_SLOTS.length ? index : -1;
    }

    private boolean isInItemGrid(double mouseX, double mouseY) {
        return contains(mouseX, mouseY, panelLeft() + 13, panelTop() + 51, ITEM_COLUMNS * SLOT_SIZE, ITEM_ROWS * SLOT_SIZE);
    }

    private int maxPage() {
        if (filteredItems.isEmpty()) {
            return 0;
        }

        return Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
    }

    private int maxStackForSelected() {
        if (selectedItem == null) {
            return 64;
        }

        return Math.max(1, new ItemStack(selectedItem).getMaxStackSize());
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private static boolean contains(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private static int[] createDisplayedSlots() {
        int[] slots = new int[36];
        for (int index = 0; index < 27; index++) {
            slots[index] = 9 + index;
        }
        for (int index = 0; index < 9; index++) {
            slots[27 + index] = index;
        }
        return slots;
    }
}
