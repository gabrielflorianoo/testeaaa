package com.gabrielflorianoo.peqol.block;

import com.gabrielflorianoo.peqol.item.AlchemicalCraftingTabletItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Alchemical Crafting Table: a crafting table that can use learned items as EMC-paid ingredients.
 */
public class AlchemicalCraftingTableBlock extends Block {

    public AlchemicalCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                 @NotNull BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            AlchemicalCraftingTabletItem.open(serverPlayer, ContainerLevelAccess.create(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
