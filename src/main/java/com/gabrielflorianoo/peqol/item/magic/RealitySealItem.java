package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reality Seal: while active and carried, explosions near you can't destroy blocks (creepers, TNT, ghasts,
 * withers...). Entities still feel the blast. Paid with EMC per block saved.
 */
public class RealitySealItem extends ToggleableItem {

    public RealitySealItem(Properties properties) {
        super(properties);
    }

    /**
     * Cancels the block damage of an explosion if a sealed player nearby can pay for it.
     */
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getAffectedBlocks().isEmpty()) {
            return;
        }
        Vec3 center = event.getExplosion().getPosition();
        double radius = PEQoLConfig.COMMON.realityRadius.get();
        int blocks = event.getAffectedBlocks().size();
        long cost = blocks * PEQoLConfig.COMMON.realityCostPerBlock.get();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) > radius * radius
                    || !InventoryUtil.contains(player, s -> s.getItem() instanceof RealitySealItem && isActive(s))) {
                continue;
            }
            if (cost > 0 && !player.getAbilities().instabuild && !EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost))) {
                continue;
            }
            event.getAffectedBlocks().clear();
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 40, 1.5, 1.5, 1.5, 0.05);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1F, 1.6F);
            player.displayClientMessage(Component.translatable("message.peqol.reality.saved", blocks, EmcUtil.format(cost)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.reality_seal.desc2", PEQoLConfig.COMMON.realityRadius.get(),
                EmcUtil.format(PEQoLConfig.COMMON.realityCostPerBlock.get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
