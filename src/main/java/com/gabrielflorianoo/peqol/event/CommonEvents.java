package com.gabrielflorianoo.peqol.event;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.StellarLinkItem;
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
import com.gabrielflorianoo.peqol.block.AlchemicalExporterBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
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
        int tick = player.tickCount;
        if (tick % PEQoLConfig.COMMON.restockerInterval.get() == 0 && hasActive(player, RestockerTalismanItem.class)) {
            RestockerTalismanItem.restock(player);
        }
        if (tick % PEQoLConfig.COMMON.mendingInterval.get() == 0 && hasActive(player, MendingCharmItem.class)) {
            MendingCharmItem.mend(player);
        }
        if (tick % PEQoLConfig.COMMON.satiationInterval.get() == 0) {
            ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof SatiationCharmItem && ToggleableItem.isActive(s));
            if (!charm.isEmpty()) {
                SatiationCharmItem.feed(player, charm);
            }
        }
        if (tick % 20 == 0 && hasActive(player, StellarLinkItem.class)) {
            StellarLinkItem.charge(player);
        }
    }

    public static boolean hasActive(net.minecraft.world.entity.player.Player player, Class<? extends ToggleableItem> type) {
        return InventoryUtil.contains(player, s -> type.isInstance(s.getItem()) && ToggleableItem.isActive(s));
    }

    /**
     * Sneak + right click with an item normally skips the block; the Exporter uses it to set stock targets.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && !event.getItemStack().isEmpty() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AlchemicalExporterBlock) {
            event.setUseBlock(Event.Result.ALLOW);
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
