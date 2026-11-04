package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
 * Recall Mirror: look into it and step out at your bed / respawn anchor (or the world spawn), across dimensions.
 */
public class RecallMirrorItem extends MagicItem {

    public RecallMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.recallCost.get())};
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel destination = serverPlayer.server.getLevel(serverPlayer.getRespawnDimension());
        BlockPos respawn = serverPlayer.getRespawnPosition();
        Optional<Vec3> spot = Optional.empty();
        boolean bed = false;
        if (destination != null && respawn != null) {
            //keepEverything = false: never uses up respawn anchor charges
            spot = Player.findRespawnPositionAndUseSpawnBlock(destination, respawn, serverPlayer.getRespawnAngle(), serverPlayer.isRespawnForced(), false);
            bed = spot.isPresent();
        }
        if (spot.isEmpty()) {
            destination = serverPlayer.server.overworld();
            spot = Optional.of(Vec3.atBottomCenterOf(destination.getSharedSpawnPos()));
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.recallCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        Vec3 target = spot.get();
        serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 80, 0.5, 1, 0.5, 0.3);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1F, 0.5F);
        serverPlayer.teleportTo(destination, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        destination.sendParticles(ParticleTypes.REVERSE_PORTAL, target.x, target.y + 1, target.z, 80, 0.5, 1, 0.5, 0.05);
        destination.playSound(null, BlockPos.containing(target), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.2F);
        player.displayClientMessage(Component.translatable(bed ? "message.peqol.recall.bed" : "message.peqol.recall.spawn").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.getCooldowns().addCooldown(this, PEQoLConfig.COMMON.recallCooldown.get());
        return InteractionResultHolder.success(stack);
    }
}
