package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlocks;
import com.gabrielflorianoo.peqol.util.EmcUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Cloud Scepter: conjures a 3x3 cloud platform under your feet (sneak: a single cloud block in front of you).
 * Clouds fade away on their own.
 */
public class CloudScepterItem extends MagicItem {

    public CloudScepterItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.cloudLifetime.get() / 20, EmcUtil.format(PEQoLConfig.COMMON.cloudCostPerBlock.get())};
    }

    /**
     * Free (air) positions of the platform.
     */
    public static List<BlockPos> platform(Level level, BlockPos center, boolean single) {
        List<BlockPos> result = new ArrayList<>();
        int r = single ? 0 : 1;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, 0, r))) {
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        boolean single = player.isShiftKeyDown();
        BlockPos center = single ? BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2.5))) : BlockPos.containing(player.position().subtract(0, 0.5, 0));
        List<BlockPos> free = platform(level, center, single);
        free.removeIf(pos -> !level.mayInteract(player, pos));
        if (free.isEmpty() || !pay(serverPlayer, free.size() * PEQoLConfig.COMMON.cloudCostPerBlock.get())) {
            return InteractionResultHolder.fail(stack);
        }
        for (BlockPos pos : free) {
            level.setBlock(pos, ModBlocks.CONJURED_CLOUD.get().defaultBlockState(), Block.UPDATE_ALL);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.01);
        }
        player.fallDistance = 0;
        level.playSound(null, center, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1F, 0.7F);
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
        player.getCooldowns().addCooldown(this, 6);
        return InteractionResultHolder.success(stack);
    }
}
