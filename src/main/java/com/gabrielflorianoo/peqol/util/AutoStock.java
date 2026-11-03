package com.gabrielflorianoo.peqol.util;

import com.gabrielflorianoo.peqol.registry.ModStats;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Per-player "always keep N of this item" list, filled from personal EMC while an Alchemical Restocker is active.
 * Stored in the Forge persisted player data so it survives death.
 */
public final class AutoStock {

    private static final String KEY = "peqol_autostock";

    private AutoStock() {
    }

    private static CompoundTag root(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag data = persisted.getCompound(KEY);
        persisted.put(KEY, data);
        return data;
    }

    /**
     * @return item id -> amount to keep, in insertion order.
     */
    public static Map<ResourceLocation, Integer> get(Player player) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        CompoundTag data = root(player);
        for (String key : data.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int amount = data.getInt(key);
            if (id != null && amount > 0) {
                result.put(id, amount);
            }
        }
        return result;
    }

    public static void set(Player player, ResourceLocation id, int amount) {
        CompoundTag data = root(player);
        if (amount <= 0) {
            data.remove(id.toString());
        } else {
            data.putInt(id.toString(), amount);
        }
    }

    public static void clear(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(KEY);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static int count(Inventory inventory, Item item) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(item) && !stack.hasTag()) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.is(item) && !stack.hasTag()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Tops every entry up to its target amount. Called periodically while a Restocker is active.
     */
    public static void tick(ServerPlayer player) {
        Map<ResourceLocation, Integer> entries = get(player);
        if (entries.isEmpty()) {
            return;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        BigInteger emc = provider.getEmc();
        BigInteger start = emc;
        for (Map.Entry<ResourceLocation, Integer> entry : entries.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item == null) {
                continue;
            }
            ItemStack template = new ItemStack(item);
            if (template.isEmpty() || EmcUtil.isBlacklisted(template) || !provider.hasKnowledge(template)) {
                continue;
            }
            int missing = entry.getValue() - count(inventory, item);
            long unit = EmcUtil.value(template);
            if (missing <= 0 || unit <= 0) {
                continue;
            }
            int amount = (int) Math.min(missing, EmcUtil.affordable(emc, unit));
            while (amount > 0) {
                int batch = Math.min(amount, template.getMaxStackSize());
                ItemStack toAdd = template.copyWithCount(batch);
                inventory.add(toAdd);
                int inserted = batch - toAdd.getCount();
                if (inserted <= 0) {
                    //Inventory full
                    break;
                }
                emc = emc.subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(inserted)));
                player.awardStat(ModStats.ITEMS_CREATED, inserted);
                amount -= inserted;
                if (inserted < batch) {
                    break;
                }
            }
        }
        if (!emc.equals(start)) {
            provider.setEmc(emc);
            provider.syncEmc(player);
        }
    }
}
