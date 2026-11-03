package com.gabrielflorianoo.peqol.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Small per-player data (homes, favorites) stored in Forge's persisted player NBT so it survives death.
 */
public final class PlayerData {

    private static final String HOMES = "peqol_homes";
    private static final String FAVORITES = "peqol_favorites";

    private PlayerData() {
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }

    // ---- Homes ----

    public static Map<String, GlobalPos> homes(Player player) {
        Map<String, GlobalPos> result = new LinkedHashMap<>();
        CompoundTag homes = persisted(player).getCompound(HOMES);
        for (String name : homes.getAllKeys()) {
            CompoundTag home = homes.getCompound(name);
            ResourceLocation dimension = ResourceLocation.tryParse(home.getString("Dim"));
            if (dimension != null) {
                result.put(name, GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(home.getLong("Pos"))));
            }
        }
        return result;
    }

    public static void setHome(Player player, String name, GlobalPos pos) {
        CompoundTag persisted = persisted(player);
        CompoundTag homes = persisted.getCompound(HOMES);
        CompoundTag home = new CompoundTag();
        home.putString("Dim", pos.dimension().location().toString());
        home.putLong("Pos", pos.pos().asLong());
        homes.put(name, home);
        persisted.put(HOMES, homes);
    }

    public static boolean removeHome(Player player, String name) {
        CompoundTag persisted = persisted(player);
        CompoundTag homes = persisted.getCompound(HOMES);
        if (!homes.contains(name)) {
            return false;
        }
        homes.remove(name);
        persisted.put(HOMES, homes);
        return true;
    }

    // ---- Favorites ----

    public static List<ResourceLocation> favorites(Player player) {
        List<ResourceLocation> result = new ArrayList<>();
        ListTag list = persisted(player).getList(FAVORITES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * @return true if added, false if it was already there (then it's removed when {@code toggle}).
     */
    public static boolean setFavorite(Player player, ResourceLocation id, boolean add) {
        CompoundTag persisted = persisted(player);
        ListTag list = persisted.getList(FAVORITES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(id.toString())) {
                if (!add) {
                    list.remove(i);
                    persisted.put(FAVORITES, list);
                    return true;
                }
                return false;
            }
        }
        if (!add) {
            return false;
        }
        list.add(StringTag.valueOf(id.toString()));
        persisted.put(FAVORITES, list);
        return true;
    }
}
