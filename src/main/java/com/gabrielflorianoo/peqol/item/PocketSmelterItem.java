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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click: instantly smelts the stack held in the other hand, paying personal EMC per item.
 */
public class PocketSmelterItem extends Item {

    public PocketSmelterItem(Properties properties) {
        super(properties);
    }

    /**
     * Result for the whole stack (any smelting recipe, not only ores), or empty.
     */
    public static ItemStack result(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<SmeltingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input.copyWithCount(1)), level);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = recipe.get().getResultItem(level.registryAccess());
        long total = (long) out.getCount() * input.getCount();
        if (out.isEmpty() || total > out.getMaxStackSize()) {
            return ItemStack.EMPTY;
        }
        return out.copyWithCount((int) total);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack smelter = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(smelter);
        }
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack input = player.getItemInHand(otherHand);
        ItemStack output = result(level, input);
        if (output.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.peqol.smelter.nothing").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(smelter);
        }
        long cost = PEQoLConfig.COMMON.smeltingCostPerItem.get() * input.getCount();
        if (cost > 0 && !player.getAbilities().instabuild && !EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(cost))) {
            player.displayClientMessage(Component.translatable("command.peqol.not_enough_emc").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(smelter);
        }
        int count = input.getCount();
        player.setItemInHand(otherHand, output);
        serverPlayer.awardStat(ModStats.ITEMS_SMELTED, count);
        level.playSound(null, player.blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 1F, 1.2F);
        player.displayClientMessage(Component.translatable("message.peqol.smelter.done", count, output.getHoverName(), EmcUtil.format(cost)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.success(smelter);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.pocket_smelter.desc", PEQoLConfig.COMMON.smeltingCostPerItem.get()).withStyle(ChatFormatting.GRAY));
    }
}
