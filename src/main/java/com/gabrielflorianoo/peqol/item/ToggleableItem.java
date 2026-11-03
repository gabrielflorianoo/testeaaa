package com.gabrielflorianoo.peqol.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An item that can be switched on and off with a right click (sneak + right click is left to subclasses).
 */
public abstract class ToggleableItem extends Item {

    public static final String TAG_ACTIVE = "Active";

    protected ToggleableItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVE, active);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isActive(stack);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return useSneaking(level, player, hand, stack);
        }
        if (!level.isClientSide) {
            boolean active = !isActive(stack);
            setActive(stack, active);
            player.displayClientMessage(Component.translatable(active ? "message.peqol.activated" : "message.peqol.deactivated", stack.getHoverName())
                    .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, active ? 1.2F : 0.6F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    protected InteractionResultHolder<ItemStack> useSneaking(Level level, Player player, InteractionHand hand, ItemStack stack) {
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(isActive(stack) ? "tooltip.peqol.active" : "tooltip.peqol.inactive")
                .withStyle(isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        String key = getDescriptionId() + ".desc";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
