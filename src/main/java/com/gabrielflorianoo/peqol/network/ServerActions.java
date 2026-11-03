package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.IlluminationCharmItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.StellarLinkItem;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkHooks;

/**
 * Server side implementation of the keybind actions.
 */
public final class ServerActions {

    private ServerActions() {
    }

    public static void handle(ServerPlayer player, KeyAction action) {
        switch (action) {
            case OPEN_TABLET -> openTablet(player);
            case OPEN_BAG -> openBag(player);
            case QUICK_BURN -> quickBurn(player);
            case TOGGLE_RESTOCKER -> toggle(player, RestockerTalismanItem.class);
            case TOGGLE_SIPHON -> toggle(player, SiphonCharmItem.class);
            case TOGGLE_MENDING -> toggle(player, MendingCharmItem.class);
            case TOGGLE_SATIATION -> toggle(player, SatiationCharmItem.class);
            case TOGGLE_STELLAR -> toggle(player, StellarLinkItem.class);
            case TOGGLE_ILLUMINATION -> toggle(player, IlluminationCharmItem.class);
        }
    }

    public static void openTablet(ServerPlayer player) {
        if (PEQoLConfig.COMMON.keybindTabletRequiresItem.get() && !EmcUtil.hasTablet(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.need_tablet").withStyle(ChatFormatting.RED), true);
            return;
        }
        //Open the "placed table" variant of the container so the tablet doesn't need to be in hand
        NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, inventory, p) -> new TransmutationContainer(windowId, inventory),
                Component.translatable("transmutation.projecte.transmute")), buf -> buf.writeBoolean(false));
    }

    public static void openBag(ServerPlayer player) {
        ItemStack bagStack = InventoryUtil.findFirstStack(player, stack -> stack.getItem() instanceof AlchemicalBag);
        DyeColor color;
        Component title;
        if (!bagStack.isEmpty()) {
            color = ((AlchemicalBag) bagStack.getItem()).color;
            title = bagStack.getHoverName();
        } else if (!PEQoLConfig.COMMON.keybindBagRequiresItem.get()) {
            color = DyeColor.WHITE;
            title = Component.translatable("container.peqol.alchemical_bag");
        } else {
            player.displayClientMessage(Component.translatable("message.peqol.need_bag").withStyle(ChatFormatting.RED), true);
            return;
        }
        IAlchBagProvider provider = player.getCapability(PECapabilities.ALCH_BAG_CAPABILITY).resolve().orElse(null);
        if (provider == null || !(provider.getBag(color) instanceof IItemHandlerModifiable inventory)) {
            return;
        }
        //Server side: no hand => the container stays valid no matter what the player holds.
        //Client side: we have to send a hand, the offhand is the one with the least restrictions.
        NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, playerInventory, p) ->
                        new AlchBagContainer(windowId, playerInventory, null, inventory, 0, false), title),
                buf -> {
                    buf.writeEnum(InteractionHand.OFF_HAND);
                    buf.writeByte(0);
                    buf.writeBoolean(false);
                });
    }

    public static void quickBurn(ServerPlayer player) {
        if (!PEQoLConfig.COMMON.quickBurnEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.peqol.disabled").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (PEQoLConfig.COMMON.quickBurnRequiresTablet.get() && !EmcUtil.hasTablet(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.need_tablet").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || EmcUtil.isTablet(held) || held.getItem() instanceof EmcChequeItem) {
            return;
        }
        if (held.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
            //Never destroy Klein Stars & co with their stored EMC
            player.displayClientMessage(Component.translatable("message.peqol.burn_emc_holder").withStyle(ChatFormatting.RED), true);
            return;
        }
        BigInteger gained = EmcUtil.burn(player, held, true);
        if (gained.signum() <= 0) {
            player.displayClientMessage(Component.translatable("message.peqol.no_emc", held.getHoverName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        Component name = held.getHoverName();
        int count = held.getCount();
        player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        player.displayClientMessage(Component.translatable("message.peqol.burned", count, name, EmcUtil.format(gained)).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    private static void toggle(ServerPlayer player, Class<? extends ToggleableItem> type) {
        ItemStack stack = InventoryUtil.findFirstStack(player, s -> type.isInstance(s.getItem()));
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.peqol.item_not_found").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean active = !ToggleableItem.isActive(stack);
        ToggleableItem.setActive(stack, active);
        player.displayClientMessage(Component.translatable(active ? "message.peqol.activated" : "message.peqol.deactivated", stack.getHoverName())
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, active ? 1.2F : 0.6F);
    }
}
