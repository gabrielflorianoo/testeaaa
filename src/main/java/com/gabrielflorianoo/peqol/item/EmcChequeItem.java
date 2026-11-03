package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.registry.ModItems;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * A tradeable piece of paper that holds personal EMC. Created with /peqol withdraw, redeemed with right click.
 * Has no EMC value of its own so it can't be burnt for profit.
 */
public class EmcChequeItem extends Item {

    public static final String TAG_EMC = "ChequeEmc";
    public static final String TAG_ISSUER = "Issuer";

    public EmcChequeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(BigInteger amount, @Nullable String issuer) {
        ItemStack stack = new ItemStack(ModItems.EMC_CHEQUE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_EMC, amount.toString());
        if (issuer != null) {
            tag.putString(TAG_ISSUER, issuer);
        }
        return stack;
    }

    public static BigInteger getAmount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_EMC)) {
            return BigInteger.ZERO;
        }
        try {
            BigInteger value = new BigInteger(tag.getString(TAG_EMC));
            return value.signum() < 0 ? BigInteger.ZERO : value;
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BigInteger amount = getAmount(stack);
        if (amount.signum() <= 0) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int count = player.isShiftKeyDown() ? stack.getCount() : 1;
            BigInteger total = amount.multiply(BigInteger.valueOf(count));
            EmcUtil.addEmc(serverPlayer, total);
            if (!player.getAbilities().instabuild) {
                stack.shrink(count);
            }
            player.displayClientMessage(Component.translatable("message.peqol.cheque.redeemed", EmcUtil.format(total)).withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        BigInteger amount = getAmount(stack);
        if (amount.signum() > 0) {
            return Component.translatable("item.peqol.emc_cheque.amount", EmcUtil.compact(amount));
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getAmount(stack).signum() > 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        BigInteger amount = getAmount(stack);
        tooltip.add(Component.translatable("tooltip.peqol.cheque.value", EmcUtil.format(amount)).withStyle(ChatFormatting.YELLOW));
        if (stack.getCount() > 1) {
            tooltip.add(Component.translatable("tooltip.peqol.cheque.stack_value", EmcUtil.format(amount.multiply(BigInteger.valueOf(stack.getCount()))))
                    .withStyle(ChatFormatting.GOLD));
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_ISSUER)) {
            tooltip.add(Component.translatable("tooltip.peqol.cheque.issuer", tag.getString(TAG_ISSUER)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.peqol.emc_cheque.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
