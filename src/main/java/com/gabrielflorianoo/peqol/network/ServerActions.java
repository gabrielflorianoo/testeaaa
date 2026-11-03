package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.IlluminationCharmItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.SmeltingCharmItem;
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
            case TOGGLE_SMELTING -> toggle(player, SmeltingCharmItem.class);
            case LEARN_INVENTORY -> learnInventory(player, true);
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
        ItemStack template = held.copy();
        player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        if (player.isShiftKeyDown()) {
            //Sneaking: burn every identical stack in the main inventory too
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (ItemStack.isSameItemSameTags(stack, template)) {
                    count += stack.getCount();
                    gained = gained.add(EmcUtil.burn(player, stack, false));
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.peqol.burned", count, name, EmcUtil.format(gained)).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    /**
     * Burns exactly one of every not-yet-learned item with EMC in the main inventory, so it becomes learned
     * while the rest of the stack is kept.
     *
     * @return the amount of newly learned items.
     */
    public static int learnInventory(ServerPlayer player, boolean feedback) {
        if (PEQoLConfig.COMMON.burnCommandRequiresTablet.get() && !EmcUtil.hasTablet(player)) {
            if (feedback) {
                player.displayClientMessage(Component.translatable("message.peqol.need_tablet").withStyle(ChatFormatting.RED), true);
            }
            return 0;
        }
        moze_intel.projecte.api.capabilities.IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return 0;
        }
        int learned = 0;
        BigInteger gained = BigInteger.ZERO;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.isEmpty() || EmcUtil.isTablet(stack) || stack.getItem() instanceof EmcChequeItem || EmcUtil.isEmcHolder(stack)
                    || !EmcUtil.hasValue(stack) || provider.hasKnowledge(stack)) {
                continue;
            }
            ItemStack one = stack.split(1);
            if (EmcUtil.tryLearn(player, one)) {
                learned++;
                gained = gained.add(EmcUtil.burn(player, one, false));
            } else {
                //Learning was refused (e.g. by another mod), give it back
                stack.grow(1);
            }
        }
        if (feedback) {
            if (learned > 0) {
                player.displayClientMessage(Component.translatable("message.peqol.learned_inventory", learned, EmcUtil.format(gained))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
            } else {
                player.displayClientMessage(Component.translatable("message.peqol.learned_inventory.none").withStyle(ChatFormatting.YELLOW), true);
            }
        }
        return learned;
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
