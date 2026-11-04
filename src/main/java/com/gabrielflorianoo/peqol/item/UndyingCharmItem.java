package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, saves you from death like a Totem of Undying, paying a large amount of personal EMC.
 */
public class UndyingCharmItem extends ToggleableItem {

    public UndyingCharmItem(Properties properties) {
        super(properties);
    }

    /**
     * @return true if the player was saved.
     */
    public boolean trySave(ServerPlayer player) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        long cost = PEQoLConfig.COMMON.undyingCost.get();
        if (cost > 0 && !EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost))) {
            return false;
        }
        player.setHealth(Math.max(1F, player.getMaxHealth() / 4F));
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.getCooldowns().addCooldown(this, PEQoLConfig.COMMON.undyingCooldown.get());
        player.displayClientMessage(Component.translatable("message.peqol.undying.saved", EmcUtil.format(cost)).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.undying_charm.desc2", EmcUtil.format(PEQoLConfig.COMMON.undyingCost.get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
