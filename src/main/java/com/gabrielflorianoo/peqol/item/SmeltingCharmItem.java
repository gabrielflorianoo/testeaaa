package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModStats;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, picked up ores (or anything smeltable) arrive already smelted; each item costs a little personal EMC.
 */
public class SmeltingCharmItem extends ToggleableItem {

    public SmeltingCharmItem(Properties properties) {
        super(properties);
    }

    public static boolean shouldSmelt(ItemStack stack) {
        return !PEQoLConfig.COMMON.smeltingOnlyOres.get() || stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS);
    }

    /**
     * @return the smelted result for the whole stack, or empty if it can't (or shouldn't) be smelted.
     */
    public static ItemStack smeltResult(Level level, ItemStack stack) {
        if (stack.isEmpty() || !shouldSmelt(stack)) {
            return ItemStack.EMPTY;
        }
        Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack.copyWithCount(1)), level);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long total = (long) result.getCount() * stack.getCount();
        if (total > result.getMaxStackSize()) {
            return ItemStack.EMPTY;
        }
        return result.copyWithCount((int) total);
    }

    /**
     * @return the smelted stack (EMC already paid) or empty if nothing happened.
     */
    public static ItemStack trySmelt(ServerPlayer player, ItemStack stack) {
        ItemStack result = smeltResult(player.level(), stack);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BigInteger cost = BigInteger.valueOf(PEQoLConfig.COMMON.smeltingCostPerItem.get()).multiply(BigInteger.valueOf(stack.getCount()));
        if (cost.signum() > 0 && !EmcUtil.tryRemoveEmc(player, cost)) {
            return ItemStack.EMPTY;
        }
        player.awardStat(ModStats.ITEMS_SMELTED, stack.getCount());
        return result;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.smelting_charm.desc2", PEQoLConfig.COMMON.smeltingCostPerItem.get()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
