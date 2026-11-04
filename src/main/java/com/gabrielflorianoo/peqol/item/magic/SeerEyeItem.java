package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Eye of the Seer: every living creature nearby glows through walls for a while.
 */
public class SeerEyeItem extends MagicItem {

    public SeerEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.seerRadius.get(), PEQoLConfig.COMMON.seerDuration.get() / 20, EmcUtil.format(PEQoLConfig.COMMON.seerCost.get())};
    }

    /**
     * Makes every living entity around the player glow. Returns {total, hostile}.
     */
    public static int[] reveal(ServerPlayer player) {
        double radius = PEQoLConfig.COMMON.seerRadius.get();
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius);
        int hostile = 0;
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, PEQoLConfig.COMMON.seerDuration.get(), 0, false, false), player);
            if (entity instanceof Enemy) {
                hostile++;
            }
        }
        return new int[]{entities.size(), hostile};
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.seerCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        int[] found = reveal(serverPlayer);
        player.displayClientMessage(Component.translatable("message.peqol.seer.found", found[0], found[1]).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getEyeY(), player.getZ(), 20, 0.6, 0.4, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.success(stack);
    }
}
