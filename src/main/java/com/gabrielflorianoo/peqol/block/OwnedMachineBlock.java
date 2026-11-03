package com.gabrielflorianoo.peqol.block;

import com.gabrielflorianoo.peqol.block.entity.IOwnedMachine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base block for machines bound to the personal EMC of whoever placed them.
 */
public abstract class OwnedMachineBlock extends BaseEntityBlock {

    protected OwnedMachineBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof IOwnedMachine machine) {
            machine.getOwnerData().set(player);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.setChanged();
            }
        }
    }

    @NotNull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                 @NotNull BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof IOwnedMachine machine) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof com.gabrielflorianoo.peqol.item.MachineUpgradeItem
                && level.getBlockEntity(pos) instanceof com.gabrielflorianoo.peqol.block.entity.IUpgradeable upgradeable) {
            if (!machine.getOwnerData().canConfigure(serverPlayer)) {
                player.displayClientMessage(Component.translatable("message.peqol.not_owner", machine.getOwnerData().describe()).withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (upgradeable.getTier() >= com.gabrielflorianoo.peqol.block.entity.IUpgradeable.MAX_TIER) {
                player.displayClientMessage(Component.translatable("message.peqol.upgrade.max").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.CONSUME;
            }
            upgradeable.setTier(upgradeable.getTier() + 1);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.peqol.upgrade.done", upgradeable.getTier()).withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            return machine.onItemUse(serverPlayer, hand, held) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            machine.onSneakUse(serverPlayer);
            return InteractionResult.CONSUME;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        machine.addInfo(lines);
        for (Component line : lines) {
            player.sendSystemMessage(line);
        }
        return InteractionResult.CONSUME;
    }
}
