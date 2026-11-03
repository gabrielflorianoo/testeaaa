package com.gabrielflorianoo.peqol.util;

import moze_intel.projecte.api.event.EMCRemapEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Caches how many registered items have an EMC value (recomputed whenever ProjectE remaps EMC).
 */
public final class KnowledgeStats {

    private static int itemsWithEmc = -1;

    private KnowledgeStats() {
    }

    public static int itemsWithEmc() {
        if (itemsWithEmc < 0) {
            int count = 0;
            for (Item item : ForgeRegistries.ITEMS) {
                if (EmcUtil.value(new ItemStack(item)) > 0) {
                    count++;
                }
            }
            itemsWithEmc = count;
        }
        return itemsWithEmc;
    }

    @SubscribeEvent
    public static void onRemap(EMCRemapEvent event) {
        itemsWithEmc = -1;
    }
}
