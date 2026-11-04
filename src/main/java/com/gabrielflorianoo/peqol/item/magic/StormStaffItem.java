package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Storm Staff: calls a lightning bolt down on the spot you look at. Sneak to make it a harmless flash (no fire, no damage).
 */
public class StormStaffItem extends MagicItem {

    public StormStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.stormRange.get(), EmcUtil.format(PEQoLConfig.COMMON.stormCost.get())};
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(PEQoLConfig.COMMON.stormRange.get()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.fail(stack);
        }
        BlockPos target = hit.getBlockPos().above();
        if (!level.mayInteract(player, target)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.stormCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(target));
            bolt.setCause(serverPlayer);
            bolt.setVisualOnly(player.isShiftKeyDown());
            level.addFreshEntity(bolt);
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getEyeY(), player.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
        player.getCooldowns().addCooldown(this, PEQoLConfig.COMMON.stormCooldown.get());
        return InteractionResultHolder.success(stack);
    }
}
