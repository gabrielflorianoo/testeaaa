package com.gabrielflorianoo.peqol.event;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CommonEvents {

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (player.tickCount % PEQoLConfig.COMMON.restockerInterval.get() == 0
                && InventoryUtil.contains(player, s -> s.getItem() instanceof RestockerTalismanItem && ToggleableItem.isActive(s))) {
            RestockerTalismanItem.restock(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof SiphonCharmItem && ToggleableItem.isActive(s) && SiphonCharmItem.matches(s, stack));
        if (!charm.isEmpty() && !stack.getCapability(moze_intel.projecte.api.capabilities.PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
            BigInteger gained = EmcUtil.burn(player, stack, PEQoLConfig.COMMON.siphonLearnsItems.get());
            if (gained.signum() > 0) {
                player.take(itemEntity, stack.getCount());
                itemEntity.discard();
                event.setCanceled(true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25F, 1.6F);
                return;
            }
        }
        if (PEQoLConfig.COMMON.autoLearnOnPickup.get() && EmcUtil.hasValue(stack) && !EmcUtil.isKnown(player, stack) && EmcUtil.hasTablet(player)) {
            if (EmcUtil.tryLearn(player, stack)) {
                player.displayClientMessage(Component.translatable("message.peqol.learned", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }
}
