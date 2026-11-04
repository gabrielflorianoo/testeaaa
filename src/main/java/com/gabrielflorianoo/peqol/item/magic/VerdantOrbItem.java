package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Verdant Orb: a pulse of life that bonemeals every plant around the clicked block, paid with EMC per growth.
 * Grass blocks are skipped so the area doesn't fill with tall grass.
 */
public class VerdantOrbItem extends MagicItem {

    public VerdantOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.verdantRadius.get(), EmcUtil.format(PEQoLConfig.COMMON.verdantCostPerGrowth.get())};
    }

    /**
     * Plants around {@code center} that can still grow, closest first.
     */
    public static List<BlockPos> growable(Level level, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock bonemealable && !(state.getBlock() instanceof GrassBlock)
                    && !state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK) && bonemealable.isValidBonemealTarget(level, pos, state, level.isClientSide)) {
                result.add(pos.immutable());
            }
        }
        result.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(center)));
        return result;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int grown = pulse((ServerPlayer) player, (ServerLevel) level, context.getClickedPos());
        if (grown > 0) {
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    /**
     * Grows the plants around {@code center}. Returns how many growth steps happened.
     */
    public static int pulse(ServerPlayer player, ServerLevel level, BlockPos center) {
        long cost = PEQoLConfig.COMMON.verdantCostPerGrowth.get();
        int max = PEQoLConfig.COMMON.verdantMaxGrowths.get();
        boolean free = player.getAbilities().instabuild || cost <= 0;
        int grown = 0;
        for (BlockPos pos : growable(level, center, PEQoLConfig.COMMON.verdantRadius.get())) {
            if (grown >= max) {
                break;
            }
            if (!free && EmcUtil.getEmc(player).compareTo(BigInteger.valueOf(cost)) < 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.peqol.magic.too_expensive", EmcUtil.format(cost))
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                break;
            }
            if (!level.mayInteract(player, pos)) {
                continue;
            }
            if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, pos, player)) {
                if (!free) {
                    EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost));
                }
                level.levelEvent(1505, pos, 0);
                grown++;
            }
        }
        if (grown > 0) {
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5,
                    30, PEQoLConfig.COMMON.verdantRadius.get() / 2.0, 0.4, PEQoLConfig.COMMON.verdantRadius.get() / 2.0, 0);
        }
        return grown;
    }
}
