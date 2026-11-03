package com.gabrielflorianoo.peqol.block;

import com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Customers buy with an empty hand (sneak = a whole stack). Owners use the normal machine interactions.
 */
public class TradePostBlock extends OwnedMachineBlock {

    public TradePostBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                 @NotNull BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && level.getBlockEntity(pos) instanceof TradePostBlockEntity post && !level.isClientSide
                && player instanceof ServerPlayer serverPlayer && !post.getOwnerData().isOwner(player)) {
            if (post.isBuyMode() && !player.getItemInHand(hand).isEmpty()) {
                post.sellToShop(serverPlayer, player.getItemInHand(hand));
                return InteractionResult.CONSUME;
            }
            if (!post.isBuyMode() && player.getItemInHand(hand).isEmpty()) {
                post.buy(serverPlayer, player.isShiftKeyDown());
                return InteractionResult.CONSUME;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            com.gabrielflorianoo.peqol.util.ShopRegistry.remove(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TradePostBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TRADE_POST.get(), TradePostBlockEntity::serverTick);
    }
}
