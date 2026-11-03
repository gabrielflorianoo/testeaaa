package com.gabrielflorianoo.peqol.util;

import java.util.function.Consumer;
import java.util.function.Predicate;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

/**
 * Optional Curios support, reusing ProjectE's own Curios helper so no extra dependency is needed.
 * The {@link Impl} class is only loaded when Curios is present.
 */
public final class CuriosCompat {

    private static Boolean loaded;

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        }
        return loaded;
    }

    public static ItemStack findFirst(Player player, Predicate<ItemStack> predicate) {
        return isLoaded() ? Impl.findFirst(player, predicate) : ItemStack.EMPTY;
    }

    public static void forEach(Player player, Consumer<ItemStack> consumer) {
        if (isLoaded()) {
            Impl.forEach(player, consumer);
        }
    }

    private static final class Impl {

        static ItemStack findFirst(Player player, Predicate<ItemStack> predicate) {
            IItemHandler handler = CuriosIntegration.getAll(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty() && predicate.test(stack)) {
                        return stack;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        static void forEach(Player player, Consumer<ItemStack> consumer) {
            IItemHandler handler = CuriosIntegration.getAll(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        }
    }
}
