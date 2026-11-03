package com.gabrielflorianoo.peqol.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OwnedBlockItem extends BlockItem {

    private final String descriptionKey;

    public OwnedBlockItem(Block block, Properties properties, String name) {
        super(block, properties);
        this.descriptionKey = "block.peqol." + name + ".desc";
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.peqol.owned_block").withStyle(ChatFormatting.DARK_GRAY));
    }
}
