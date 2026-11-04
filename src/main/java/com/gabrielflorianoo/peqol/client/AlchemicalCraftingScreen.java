package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu;
import com.gabrielflorianoo.peqol.network.GhostIngredientPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Crafting table on the right, a searchable panel with everything you have learned on the left. Click a learned
 * item to pick it up as a ghost ingredient, then click grid slots to place it; click a ghost to remove it.
 * Shift + click a learned item drops it into the first free slot.
 */
public class AlchemicalCraftingScreen extends AbstractContainerScreen<AlchemicalCraftingMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/crafting_table.png");
    public static final int COLUMNS = 6;
    public static final int ROWS = 7;
    public static final int PANEL_WIDTH = COLUMNS * 18 + 10;

    private EditBox search;
    private final List<ItemStack> known = new ArrayList<>();
    private final List<ItemStack> filtered = new ArrayList<>();
    private int scroll;
    private ItemStack selected = ItemStack.EMPTY;
    private int refreshTimer;
    private String lastQuery = "";

    public AlchemicalCraftingScreen(AlchemicalCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    public int panelLeft() {
        return leftPos - PANEL_WIDTH - 4;
    }

    /**
     * Area covered by the side panel (for JEI exclusion zones).
     */
    public Rect2i panelArea() {
        return new Rect2i(panelLeft(), topPos, PANEL_WIDTH, imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, panelLeft() + 5, topPos + 6, PANEL_WIDTH - 10, 12, Component.translatable("gui.peqol.alchemical_crafting.search"));
        search.setHint(Component.translatable("gui.peqol.alchemical_crafting.search").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        search.setMaxLength(50);
        search.setValue(lastQuery);
        search.setResponder(text -> {
            lastQuery = text;
            scroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("✖"), b -> PEQoLNetwork.sendToServer(
                        new GhostIngredientPacket(menu.containerId, GhostIngredientPacket.WHOLE_GRID, List.of())))
                .bounds(leftPos + 150, topPos + 4, 14, 12)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.peqol.alchemical_crafting.clear")))
                .build());
        refreshKnowledge();
    }

    private void refreshKnowledge() {
        known.clear();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(minecraft.player).orElse(null);
        if (provider == null) {
            return;
        }
        if (provider.hasFullKnowledge() || minecraft.player.getAbilities().instabuild) {
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack stack = new ItemStack(item);
                if (EmcUtil.value(stack) > 0) {
                    known.add(stack);
                }
            }
        } else {
            for (ItemInfo info : provider.getKnowledge()) {
                ItemStack stack = info.createStack();
                if (EmcUtil.value(stack) > 0) {
                    known.add(stack);
                }
            }
        }
        known.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
        applyFilter();
    }

    private void applyFilter() {
        filtered.clear();
        String query = lastQuery.toLowerCase(Locale.ROOT).trim();
        for (ItemStack stack : known) {
            if (query.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())).contains(query)) {
                filtered.add(stack);
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, (filtered.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (++refreshTimer % 40 == 0) {
            int before = known.size();
            refreshKnowledge();
            if (known.size() != before) {
                scroll = Math.min(scroll, maxScroll());
            }
        }
    }

    @Nullable
    private ItemStack entryAt(double mouseX, double mouseY) {
        int x0 = panelLeft() + 5;
        int y0 = topPos + 22;
        if (mouseX < x0 || mouseY < y0 || mouseX >= x0 + COLUMNS * 18 || mouseY >= y0 + ROWS * 18) {
            return null;
        }
        int col = (int) (mouseX - x0) / 18;
        int row = (int) (mouseY - y0) / 18;
        int index = (row + scroll) * COLUMNS + col;
        return index < filtered.size() ? filtered.get(index) : null;
    }

    private int gridIndex(@Nullable Slot slot) {
        if (slot == null) {
            return -1;
        }
        int index = menu.slots.indexOf(slot);
        return index >= AlchemicalCraftingMenu.GRID_START && index < AlchemicalCraftingMenu.INV_START ? index - AlchemicalCraftingMenu.GRID_START : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack entry = entryAt(mouseX, mouseY);
        if (entry != null) {
            if (button == 1) {
                selected = ItemStack.EMPTY;
            } else if (Screen.hasShiftDown()) {
                PEQoLNetwork.sendToServer(GhostIngredientPacket.single(menu.containerId, GhostIngredientPacket.FIRST_FREE, entry));
            } else {
                selected = ItemStack.isSameItemSameTags(selected, entry) ? ItemStack.EMPTY : entry.copyWithCount(1);
            }
            return true;
        }
        int grid = gridIndex(hoveredSlot);
        if (grid >= 0 && menu.getCarried().isEmpty()) {
            if (button == 0 && !selected.isEmpty() && (menu.isGhost(grid) || !hoveredSlot.hasItem())) {
                PEQoLNetwork.sendToServer(GhostIngredientPacket.single(menu.containerId, grid, selected));
                return true;
            }
            if (menu.isGhost(grid)) {
                PEQoLNetwork.sendToServer(GhostIngredientPacket.single(menu.containerId, grid, ItemStack.EMPTY));
                return true;
            }
        }
        if (button == 1 && !selected.isEmpty() && hoveredSlot == null && !search.isMouseOver(mouseX, mouseY)) {
            selected = ItemStack.EMPTY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelLeft() && mouseX < panelLeft() + PANEL_WIDTH && mouseY >= topPos && mouseY < topPos + imageHeight) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused() && keyCode != 256) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int x = panelLeft();
        int y = topPos;
        graphics.fill(x, y, x + PANEL_WIDTH, y + imageHeight, 0xFF1E1028);
        graphics.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + imageHeight - 1, 0xFF3A2150);
        int x0 = x + 5;
        int y0 = y + 22;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int sx = x0 + col * 18;
                int sy = y0 + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF2A1638);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF4A2E66);
                int index = (row + scroll) * COLUMNS + col;
                if (index < filtered.size()) {
                    ItemStack stack = filtered.get(index);
                    if (ItemStack.isSameItemSameTags(stack, selected)) {
                        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFB060FF);
                    }
                    graphics.renderItem(stack, sx + 1, sy + 1);
                }
            }
        }
        //Scroll bar
        int max = maxScroll();
        if (max > 0) {
            int barHeight = ROWS * 18;
            int knob = Math.max(10, barHeight * ROWS / (ROWS + max));
            int knobY = y0 + (barHeight - knob) * scroll / max;
            graphics.fill(x + PANEL_WIDTH - 4, y0, x + PANEL_WIDTH - 2, y0 + barHeight, 0xFF2A1638);
            graphics.fill(x + PANEL_WIDTH - 4, knobY, x + PANEL_WIDTH - 2, knobY + knob, 0xFFC080FF);
        }
        graphics.drawString(font, Component.translatable("gui.peqol.alchemical_crafting.known", filtered.size()), x + 5, y0 + ROWS * 18 + 4, 0xFFC8A8E8, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        long cost = menu.ghostCost();
        if (cost > 0 && minecraft != null && minecraft.player != null) {
            boolean affordable = minecraft.player.getAbilities().instabuild || EmcUtil.getEmc(minecraft.player).compareTo(BigInteger.valueOf(cost)) >= 0;
            Component text = Component.translatable("gui.peqol.alchemical_crafting.cost", EmcUtil.format(cost, true));
            graphics.drawString(font, text, 90 + (84 - font.width(text)) / 2, 60, affordable ? 0xFF2E8B2E : 0xFFB02020, false);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        //Ghost ingredients get a purple veil and a corner mark
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < 9; i++) {
            if (menu.isGhost(i)) {
                Slot slot = menu.slots.get(AlchemicalCraftingMenu.GRID_START + i);
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x509040E0);
                graphics.fill(sx + 12, sy, sx + 16, sy + 4, 0xFFC080FF);
            }
        }
        graphics.pose().popPose();
        if (!selected.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 350);
            graphics.renderItem(selected, mouseX + 6, mouseY + 6);
            graphics.pose().popPose();
        }
        ItemStack entry = entryAt(mouseX, mouseY);
        if (entry != null) {
            graphics.renderTooltip(font, entry, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
