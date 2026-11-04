package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Gravitational Staff: right click pulls every dropped item and XP orb around you to your feet;
 * sneak + right click releases a shockwave that throws hostile mobs away.
 */
public class GravityStaffItem extends MagicItem {

    public GravityStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.gravityRadius.get(), EmcUtil.format(PEQoLConfig.COMMON.gravityCost.get()),
                PEQoLConfig.COMMON.gravityRepelRadius.get(), EmcUtil.format(PEQoLConfig.COMMON.gravityRepelCost.get())};
    }

    /**
     * Brings items and XP orbs within the radius to the player. Returns how many entities moved.
     */
    public static int attract(ServerPlayer player) {
        double radius = PEQoLConfig.COMMON.gravityRadius.get();
        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> entities = player.level().getEntities(player, box, e -> (e instanceof ItemEntity || e instanceof ExperienceOrb) && e.isAlive());
        ServerLevel level = player.serverLevel();
        for (Entity entity : entities) {
            Vec3 from = entity.position();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.2, from.z, 4, 0.1, 0.1, 0.1, 0.02);
            entity.teleportTo(player.getX(), player.getY() + 0.2, player.getZ());
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity instanceof ItemEntity item) {
                item.setNoPickUpDelay();
            }
        }
        return entities.size();
    }

    /**
     * Throws hostile mobs around the player away. Returns how many were hit.
     */
    public static int repel(ServerPlayer player) {
        double radius = PEQoLConfig.COMMON.gravityRepelRadius.get();
        List<LivingEntity> mobs = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(player) <= radius * radius);
        for (LivingEntity mob : mobs) {
            Vec3 away = mob.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(1, 0, 0);
            }
            away = away.normalize();
            mob.knockback(2.5, -away.x, -away.z);
            mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.5, 0));
            mob.hurtMarked = true;
        }
        return mobs.size();
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            if (!pay(serverPlayer, PEQoLConfig.COMMON.gravityRepelCost.get())) {
                return InteractionResultHolder.fail(stack);
            }
            repel(serverPlayer);
            double r = PEQoLConfig.COMMON.gravityRepelRadius.get();
            for (int i = 0; i < 36; i++) {
                double angle = Math.PI * 2 * i / 36;
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, player.getX() + Math.cos(angle) * r * 0.5, player.getY() + 1, player.getZ() + Math.sin(angle) * r * 0.5, 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.6F);
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.success(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.gravityCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        int moved = attract(serverPlayer);
        serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 1, 1, 1, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, moved > 0 ? 1.8F : 0.8F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }
}
