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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Blink Rod: instantly teleports you to the spot you are looking at.
 */
public class BlinkRodItem extends MagicItem {

    public BlinkRodItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.blinkRange.get(), EmcUtil.format(PEQoLConfig.COMMON.blinkCostPerBlock.get())};
    }

    /**
     * Where a player looking along {@code look} from {@code eye} would land, or null.
     */
    @Nullable
    public static Vec3 destination(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        BlockPos feet;
        if (hit.getType() == HitResult.Type.BLOCK) {
            feet = hit.getBlockPos().relative(hit.getDirection());
        } else {
            feet = BlockPos.containing(end);
        }
        //Find a spot where the player fits, nudging upwards a little
        for (int i = 0; i < 3; i++) {
            BlockPos candidate = feet.above(i);
            if (level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                    && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                return Vec3.atBottomCenterOf(candidate);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Vec3 target = destination(level, player, PEQoLConfig.COMMON.blinkRange.get());
        if (target == null) {
            return InteractionResultHolder.fail(stack);
        }
        long distance = (long) Math.ceil(player.position().distanceTo(target));
        if (!pay(serverPlayer, distance * PEQoLConfig.COMMON.blinkCostPerBlock.get())) {
            return InteractionResultHolder.fail(stack);
        }
        Vec3 from = player.position();
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.02);
        //Trail between both points
        Vec3 step = target.subtract(from).scale(1.0 / Math.max(1, distance));
        for (int i = 0; i < distance; i++) {
            Vec3 p = from.add(step.scale(i));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 1, 0, 0, 0, 0);
        }
        serverPlayer.teleportTo(serverLevel, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, target.x, target.y + 1, target.z, 30, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.4F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }
}
