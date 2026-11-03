package com.gabrielflorianoo.peqol.util;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Searches main inventory (hotbar first) and offhand.
     *
     * @return the slot index in {@link Inventory#getItem(int)} terms or -1.
     */
    public static int findFirst(Player player, Predicate<ItemStack> predicate) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (predicate.test(inventory.items.get(i))) {
                return i;
            }
        }
        for (int i = 0; i < inventory.offhand.size(); i++) {
            if (predicate.test(inventory.offhand.get(i))) {
                return inventory.items.size() + inventory.armor.size() + i;
            }
        }
        return -1;
    }

    public static ItemStack findFirstStack(Player player, Predicate<ItemStack> predicate) {
        int slot = findFirst(player, predicate);
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    public static boolean contains(Player player, Predicate<ItemStack> predicate) {
        return findFirst(player, predicate) >= 0;
    }

    /**
     * Inserts the stack into the player's inventory, dropping whatever doesn't fit.
     */
    public static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}
