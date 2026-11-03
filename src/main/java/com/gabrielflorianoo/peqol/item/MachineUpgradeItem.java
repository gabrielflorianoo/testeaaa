package com.gabrielflorianoo.peqol.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click an Exporter, Vacuum or Downlink to raise its tier (handled by the machine block).
 */
public class MachineUpgradeItem extends Item {

    public MachineUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.machine_upgrade.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.machine_upgrade.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
