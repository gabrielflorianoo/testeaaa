package com.gabrielflorianoo.peqol.util;

import java.util.function.Consumer;
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

    /**
     * Searches main inventory, offhand and (if installed) Curios slots.
     */
    public static ItemStack findFirstStack(Player player, Predicate<ItemStack> predicate) {
        int slot = findFirst(player, predicate);
        if (slot >= 0) {
            return player.getInventory().getItem(slot);
        }
        return CuriosCompat.findFirst(player, predicate);
    }

    public static boolean contains(Player player, Predicate<ItemStack> predicate) {
        return !findFirstStack(player, predicate).isEmpty();
    }

    /**
     * Visits every non-empty stack: main inventory, armor, offhand and Curios.
     */
    public static void forEachStack(Player player, Consumer<ItemStack> consumer) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                consumer.accept(stack);
            }
        }
        CuriosCompat.forEach(player, consumer);
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
