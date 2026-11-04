package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Lumen Wand: conjures invisible light sources (vanilla light blocks). Sneak + right click dispels nearby ones.
 * While held, the hidden lights sparkle so you can see them.
 */
public class LumenWandItem extends MagicItem {

    public static final int DISPEL_RADIUS = 8;

    public LumenWandItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.lumenCost.get())};
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return placeLight((ServerPlayer) player, (ServerLevel) level, target) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            int removed = dispel(serverLevel, player.blockPosition());
            if (removed > 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            return InteractionResultHolder.success(stack);
        }
        //Right click in the air: light up the spot two blocks in front of the eyes
        BlockPos target = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2)));
        placeLight(serverPlayer, serverLevel, target);
        return InteractionResultHolder.success(stack);
    }

    public static boolean placeLight(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        boolean water = existing.is(Blocks.WATER) && existing.getFluidState().isSource();
        if (!existing.isAir() && !water || !level.mayInteract(player, pos)) {
            return false;
        }
        if (!pay(player, PEQoLConfig.COMMON.lumenCost.get())) {
            return false;
        }
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15)
                .setValue(LightBlock.WATERLOGGED, water);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        level.setBlock(pos, light, Block.UPDATE_ALL);
        if (ForgeEventFactory.onBlockPlace(player, snapshot, net.minecraft.core.Direction.UP)) {
            snapshot.restore(true, false);
            return false;
        }
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    /**
     * Removes the light blocks around {@code center}.
     */
    public static int dispel(ServerLevel level, BlockPos center) {
        int removed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-DISPEL_RADIUS, -DISPEL_RADIUS, -DISPEL_RADIUS), center.offset(DISPEL_RADIUS, DISPEL_RADIUS, DISPEL_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LIGHT)) {
                level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.01);
                removed++;
            }
        }
        return removed;
    }
}
