package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click: skip to the next morning. Sneak + right click: clear the weather. Both paid with personal EMC.
 */
public class CelestialHourglassItem extends Item {

    public CelestialHourglassItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!PEQoLConfig.COMMON.hourglassEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.peqol.disabled").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean weather = player.isShiftKeyDown();
        if (weather && !serverLevel.isRaining() && !serverLevel.isThundering()) {
            player.displayClientMessage(Component.translatable("message.peqol.hourglass.clear_already").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!weather && serverLevel.isDay()) {
            player.displayClientMessage(Component.translatable("message.peqol.hourglass.day_already").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        long cost = weather ? PEQoLConfig.COMMON.hourglassWeatherCost.get() : PEQoLConfig.COMMON.hourglassDayCost.get();
        if (cost > 0 && !player.getAbilities().instabuild && !EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(cost))) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.too_expensive", EmcUtil.format(cost)).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (weather) {
            serverLevel.setWeatherParameters(12000, 0, false, false);
        } else {
            long time = serverLevel.getDayTime();
            long nextMorning = (time / 24000L + 1) * 24000L;
            serverLevel.setDayTime(nextMorning);
        }
        serverPlayer.getCooldowns().addCooldown(this, 100);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, weather ? 1.6F : 1.2F);
        serverPlayer.server.getPlayerList().broadcastSystemMessage(Component.translatable(weather ? "message.peqol.hourglass.cleared" : "message.peqol.hourglass.morning",
                player.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.celestial_hourglass.desc", EmcUtil.format(PEQoLConfig.COMMON.hourglassDayCost.get())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.celestial_hourglass.desc2", EmcUtil.format(PEQoLConfig.COMMON.hourglassWeatherCost.get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
