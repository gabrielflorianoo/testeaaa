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
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * World-wide list of Trade Posts so /peqol shops works even for unloaded chunks.
 */
public class ShopRegistry extends SavedData {

    private static final String NAME = "peqol_shops";

    public record Entry(GlobalPos pos, String owner, String product, String price, boolean buying) {
    }

    private final Map<GlobalPos, Entry> entries = new LinkedHashMap<>();

    @Nullable
    public static ShopRegistry get(@Nullable MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return server.overworld().getDataStorage().computeIfAbsent(ShopRegistry::load, ShopRegistry::new, NAME);
    }

    public static void update(Level level, BlockPos pos, String owner, String product, String price, boolean buying) {
        ShopRegistry registry = get(level.getServer());
        if (registry != null) {
            GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
            registry.entries.put(key, new Entry(key, owner, product, price, buying));
            registry.setDirty();
        }
    }

    public static void remove(Level level, BlockPos pos) {
        ShopRegistry registry = get(level.getServer());
        if (registry != null && registry.entries.remove(GlobalPos.of(level.dimension(), pos.immutable())) != null) {
            registry.setDirty();
        }
    }

    public List<Entry> entries() {
        return new ArrayList<>(entries.values());
    }

    private static ShopRegistry load(CompoundTag tag) {
        ShopRegistry registry = new ShopRegistry();
        ListTag list = tag.getList("Shops", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dim"));
            if (dimension == null) {
                continue;
            }
            GlobalPos pos = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("Pos")));
            registry.entries.put(pos, new Entry(pos, entry.getString("Owner"), entry.getString("Product"), entry.getString("Price"), entry.getBoolean("Buying")));
        }
        return registry;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag compound = new CompoundTag();
            compound.putString("Dim", entry.pos().dimension().location().toString());
            compound.putLong("Pos", entry.pos().pos().asLong());
            compound.putString("Owner", entry.owner());
            compound.putString("Product", entry.product());
            compound.putString("Price", entry.price());
            compound.putBoolean("Buying", entry.buying());
            list.add(compound);
        }
        tag.put("Shops", list);
        return tag;
    }
}
