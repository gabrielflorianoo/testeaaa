package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Pendant of the Temporal Echo: while carried it remembers where you were; right click rewinds you a few seconds
 * back in time (position, and health if it was higher).
 */
public class ChronalEchoItem extends MagicItem {

    public record Echo(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot, float health) {
    }

    private static final Map<UUID, Deque<Echo>> ECHOES = new HashMap<>();
    private static final int MAX_SECONDS = 30;

    public ChronalEchoItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.echoSeconds.get(), EmcUtil.format(PEQoLConfig.COMMON.echoCost.get())};
    }

    /**
     * Called once per second for players carrying the pendant.
     */
    public static void record(ServerPlayer player) {
        Deque<Echo> echoes = ECHOES.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        echoes.addLast(new Echo(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(), player.getHealth()));
        while (echoes.size() > MAX_SECONDS) {
            echoes.removeFirst();
        }
    }

    public static void forget(UUID player) {
        ECHOES.remove(player);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Deque<Echo> echoes = ECHOES.get(player.getUUID());
        int seconds = PEQoLConfig.COMMON.echoSeconds.get();
        if (echoes == null || echoes.size() < 2) {
            player.displayClientMessage(Component.translatable("message.peqol.echo.none").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        Echo[] history = echoes.toArray(new Echo[0]);
        Echo echo = history[Math.max(0, history.length - 1 - seconds)];
        if (echo.dimension() != level.dimension()) {
            player.displayClientMessage(Component.translatable("message.peqol.echo.dimension").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.echoCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        Vec3 from = player.position();
        serverLevel.sendParticles(ParticleTypes.ENCHANT, from.x, from.y + 1, from.z, 60, 0.4, 0.8, 0.4, 0.5);
        serverPlayer.teleportTo(serverLevel, echo.pos().x, echo.pos().y, echo.pos().z, echo.yRot(), echo.xRot());
        player.fallDistance = 0;
        player.clearFire();
        if (echo.health() > player.getHealth()) {
            player.setHealth(echo.health());
        }
        serverLevel.sendParticles(ParticleTypes.ENCHANT, echo.pos().x, echo.pos().y + 1, echo.pos().z, 60, 0.4, 0.8, 0.4, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        echoes.clear();
        player.getCooldowns().addCooldown(this, PEQoLConfig.COMMON.echoCooldown.get());
        player.displayClientMessage(Component.translatable("message.peqol.echo.done", seconds).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResultHolder.success(stack);
    }
}
