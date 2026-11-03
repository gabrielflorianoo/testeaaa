package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens your ender chest from anywhere, for a small EMC fee.
 */
public class VoidPouchItem extends Item {

    public VoidPouchItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        long fee = PEQoLConfig.COMMON.voidPouchCost.get();
        if (fee > 0 && !player.getAbilities().instabuild && !EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(fee))) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.too_expensive", EmcUtil.format(fee)).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                Component.translatable("container.enderchest")));
        player.awardStat(Stats.OPEN_ENDERCHEST);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5F, 1.2F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.void_pouch.desc", EmcUtil.format(PEQoLConfig.COMMON.voidPouchCost.get())).withStyle(ChatFormatting.GRAY));
    }
}
