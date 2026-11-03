package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.menu.FilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Ghost-slot filter screen (vanilla 3-row chest look) with an optional junk mode toggle.
 */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int ROWS = 3;
    private Button junkButton;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.hasJunkOption()) {
            junkButton = addRenderableWidget(Button.builder(junkLabel(), b -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.gameMode != null) {
                            mc.gameMode.handleInventoryButtonClick(menu.containerId, FilterMenu.BUTTON_JUNK);
                        }
                    })
                    .bounds(leftPos + imageWidth + 4, topPos + 4, 90, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.peqol.junk.tooltip")))
                    .build());
        }
    }

    private Component junkLabel() {
        return Component.translatable(menu.isJunkMode() ? "gui.peqol.junk.on" : "gui.peqol.junk.off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (junkButton != null) {
            junkButton.setMessage(junkLabel());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, ROWS * 18 + 17);
        graphics.blit(TEXTURE, leftPos, topPos + ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }
}
