package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Prismatic Wand: fires a rainbow beam that strikes the first creature in its path with magic damage.
 */
public class PrismaticWandItem extends MagicItem {

    private static final Vector3f[] COLORS = {
            new Vector3f(1F, 0.2F, 0.2F), new Vector3f(1F, 0.6F, 0.1F), new Vector3f(1F, 1F, 0.2F),
            new Vector3f(0.2F, 1F, 0.3F), new Vector3f(0.2F, 0.6F, 1F), new Vector3f(0.7F, 0.3F, 1F)
    };

    public PrismaticWandItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.prismDamage.get(), EmcUtil.format(PEQoLConfig.COMMON.prismCost.get())};
    }

    /**
     * First living entity hit by a beam from the player's eyes, stopping at blocks.
     */
    @Nullable
    public static LivingEntity target(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        Vec3 blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        AABB box = player.getBoundingBox().expandTowards(blockHit.subtract(eye)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, blockHit, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable());
        if (hit == null) {
            return null;
        }
        Entity entity = hit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.prismCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        double range = PEQoLConfig.COMMON.prismRange.get();
        LivingEntity target = target(level, player, range);
        Vec3 eye = player.getEyePosition().subtract(0, 0.2, 0);
        Vec3 end = target != null ? target.getBoundingBox().getCenter()
                : level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        double length = end.distanceTo(eye);
        Vec3 dir = end.subtract(eye).normalize();
        for (double d = 0.8; d < length; d += 0.35) {
            Vec3 p = eye.add(dir.scale(d));
            Vector3f color = COLORS[(int) (d * 2) % COLORS.length];
            serverLevel.sendParticles(new DustParticleOptions(color, 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (target != null) {
            target.hurt(level.damageSources().indirectMagic(player, player), PEQoLConfig.COMMON.prismDamage.get().floatValue());
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, end.x, end.y, end.z, 12, 0.2, 0.2, 0.2, 0.08);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 1.8F);
        player.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.success(stack);
    }
}
