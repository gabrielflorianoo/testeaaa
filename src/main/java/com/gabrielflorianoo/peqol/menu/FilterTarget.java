package com.gabrielflorianoo.peqol.menu;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where a {@link FilterMenu} reads and writes its ghost items (an item in hand, a block entity...).
 */
public interface FilterTarget {

    List<ItemStack> load();

    void save(List<ItemStack> filter);

    boolean stillValid(Player player);

    default boolean hasJunkMode() {
        return false;
    }

    default boolean isJunkMode() {
        return false;
    }

    default void setJunkMode(boolean junk) {
    }
}
