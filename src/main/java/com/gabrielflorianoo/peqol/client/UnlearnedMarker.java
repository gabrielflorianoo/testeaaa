package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;

/**
 * Draws a small dot on every slot holding an item that has EMC but isn't learned yet.
 */
public final class UnlearnedMarker {

    private static final int COLOR = 0xFFB04BFF;

    private UnlearnedMarker() {
    }

    public static void render(ContainerScreenEvent.Render.Foreground event) {
        if (!PEQoLConfig.CLIENT.markUnlearned.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(mc.player).orElse(null);
        if (provider == null || provider.hasFullKnowledge()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !EmcUtil.hasValue(stack) || provider.hasKnowledge(stack)) {
                continue;
            }
            graphics.fill(slot.x, slot.y, slot.x + 3, slot.y + 3, COLOR);
        }
        graphics.pose().popPose();
    }
}
