package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Void Sponge: drains every fluid (water, lava, modded fluids) in a sphere around the spot you look at, and dries
 * waterlogged blocks. Paid with EMC per block.
 */
public class VoidSpongeItem extends MagicItem {

    public VoidSpongeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.spongeRadius.get(), EmcUtil.format(PEQoLConfig.COMMON.spongeCostPerBlock.get())};
    }

    /**
     * Fluid positions inside the sphere around {@code center}, closest first.
     */
    public static List<BlockPos> fluids(Level level, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) <= radius * radius + 1 && !level.getFluidState(pos).isEmpty()) {
                result.add(pos.immutable());
            }
        }
        result.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(center)));
        return result;
    }

    /**
     * Removes the fluid at {@code pos}. Returns whether something was drained.
     */
    public static boolean drain(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.getBlock() instanceof BucketPickup pickup && !(state.getBlock() instanceof LiquidBlock)) {
            //Waterlogged blocks keep the block, lose the water
            return !pickup.pickupBlock(level, pos, state).isEmpty() || level.getFluidState(pos).isEmpty();
        }
        if (state.getBlock() instanceof LiquidBlock || state.is(Blocks.BUBBLE_COLUMN)) {
            return level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (state.canBeReplaced()) {
            //Kelp, seagrass and friends
            return level.destroyBlock(pos, true);
        }
        return false;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        BlockPos center = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : player.blockPosition();
        List<BlockPos> targets = fluids(level, center, PEQoLConfig.COMMON.spongeRadius.get());
        if (targets.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        long cost = PEQoLConfig.COMMON.spongeCostPerBlock.get();
        int allowed = targets.size();
        if (!player.getAbilities().instabuild && cost > 0) {
            allowed = (int) Math.min(allowed, EmcUtil.affordable(EmcUtil.getEmc(player), cost));
            if (allowed <= 0) {
                pay(serverPlayer, cost);
                return InteractionResultHolder.fail(stack);
            }
        }
        int drained = 0;
        for (BlockPos pos : targets) {
            if (drained >= allowed) {
                break;
            }
            if (level.mayInteract(player, pos) && drain(level, pos)) {
                drained++;
                if (drained % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.2);
                }
            }
        }
        if (!player.getAbilities().instabuild && drained > 0) {
            EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(cost * drained));
        }
        level.playSound(null, center, SoundEvents.SPONGE_ABSORB, SoundSource.PLAYERS, 1F, 0.7F);
        level.playSound(null, center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4F, 0.5F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }
}
