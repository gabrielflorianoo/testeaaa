package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class TooltipHandler {

    private TooltipHandler() {
    }

    public static void addLines(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (stack.isEmpty() || player == null || stack.getItem() instanceof EmcChequeItem) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        CompoundTag beData = BlockItem.getBlockEntityData(stack);
        if (beData != null && beData.contains("EMC", Tag.TAG_LONG) && beData.getLong("EMC") > 0) {
            tooltip.add(Component.translatable("tooltip.peqol.wrenched_emc", EmcUtil.format(beData.getLong("EMC"))).withStyle(ChatFormatting.YELLOW));
        }
        long value = EmcUtil.value(stack);
        if (value <= 0) {
            if (PEQoLConfig.CLIENT.tooltipNoEmcWarning.get()) {
                tooltip.add(Component.translatable("tooltip.peqol.no_emc").withStyle(ChatFormatting.DARK_GRAY));
            }
            addContainerContents(stack, tooltip);
            return;
        }
        if (!Screen.hasShiftDown()) {
            return;
        }
        boolean compact = PEQoLConfig.CLIENT.tooltipCompactNumbers.get();
        if (PEQoLConfig.CLIENT.tooltipSellValue.get()) {
            long sell = EmcUtil.sellValue(stack);
            if (sell != value) {
                tooltip.add(Component.translatable("tooltip.peqol.sell_value", EmcUtil.format(sell, compact)).withStyle(ChatFormatting.GOLD));
            }
        }
        if (PEQoLConfig.CLIENT.tooltipAffordable.get()) {
            IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
            if (provider != null && provider.hasKnowledge(stack)) {
                long affordable = EmcUtil.affordable(provider.getEmc(), value);
                ChatFormatting color = affordable > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                if (affordable >= Integer.MAX_VALUE) {
                    tooltip.add(Component.translatable("tooltip.peqol.affordable_lots").withStyle(color));
                } else {
                    int maxStack = Math.max(1, stack.getMaxStackSize());
                    if (maxStack > 1 && affordable >= maxStack) {
                        tooltip.add(Component.translatable("tooltip.peqol.affordable_stacks", EmcUtil.format(affordable, compact),
                                EmcUtil.format(affordable / maxStack, compact)).withStyle(color));
                    } else {
                        tooltip.add(Component.translatable("tooltip.peqol.affordable", EmcUtil.format(affordable, compact)).withStyle(color));
                    }
                }
            }
        }
        if (stack.getMaxStackSize() > 1 && stack.getCount() != stack.getMaxStackSize()) {
            BigInteger full = BigInteger.valueOf(value).multiply(BigInteger.valueOf(stack.getMaxStackSize()));
            tooltip.add(Component.translatable("tooltip.peqol.full_stack", stack.getMaxStackSize(), EmcUtil.format(full, compact)).withStyle(ChatFormatting.DARK_AQUA));
        }
        addContainerContents(stack, tooltip);
    }

    private static void addContainerContents(ItemStack stack, List<Component> tooltip) {
        if (!PEQoLConfig.CLIENT.tooltipContainerContents.get() || !Screen.hasShiftDown()) {
            return;
        }
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null || !data.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        ListTag items = data.getList("Items", Tag.TAG_COMPOUND);
        BigInteger total = BigInteger.ZERO;
        int count = 0;
        int withoutEmc = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack content = ItemStack.of(items.getCompound(i));
            if (content.isEmpty()) {
                continue;
            }
            count += content.getCount();
            long value = EmcUtil.value(content);
            if (value > 0) {
                total = total.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(content.getCount())));
            } else {
                withoutEmc += content.getCount();
            }
        }
        if (count == 0) {
            return;
        }
        boolean compact = PEQoLConfig.CLIENT.tooltipCompactNumbers.get();
        tooltip.add(Component.translatable("tooltip.peqol.contents", EmcUtil.format(total, compact), count).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (withoutEmc > 0) {
            tooltip.add(Component.translatable("tooltip.peqol.contents_no_emc", withoutEmc).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
