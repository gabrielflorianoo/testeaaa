package com.gabrielflorianoo.peqol.menu;

import com.gabrielflorianoo.peqol.block.entity.AlchemicalVacuumBlockEntity;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Filter targets for the Siphon Charm (item in hand) and the Alchemical Vacuum (block entity).
 */
public final class FilterTargets {

    private FilterTargets() {
    }

    static List<ItemStack> toStacks(List<ResourceLocation> ids) {
        List<ItemStack> result = new ArrayList<>();
        for (ResourceLocation id : ids) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                result.add(new ItemStack(item));
            }
        }
        return result;
    }

    static List<ResourceLocation> toIds(List<ItemStack> stacks) {
        List<ResourceLocation> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public static void openSiphon(ServerPlayer player, InteractionHand hand) {
        ItemStack charm = player.getItemInHand(hand);
        FilterTarget target = new FilterTarget() {
            @Override
            public List<ItemStack> load() {
                return toStacks(SiphonCharmItem.getFilter(charm));
            }

            @Override
            public void save(List<ItemStack> filter) {
                ListTag list = new ListTag();
                for (ResourceLocation id : toIds(filter)) {
                    list.add(StringTag.valueOf(id.toString()));
                }
                charm.getOrCreateTag().put(SiphonCharmItem.TAG_FILTER, list);
            }

            @Override
            public boolean stillValid(Player viewer) {
                return viewer.getItemInHand(hand) == charm;
            }

            @Override
            public boolean hasJunkMode() {
                return true;
            }

            @Override
            public boolean isJunkMode() {
                return SiphonCharmItem.isJunkMode(charm);
            }

            @Override
            public void setJunkMode(boolean junk) {
                charm.getOrCreateTag().putBoolean(SiphonCharmItem.TAG_JUNK, junk);
            }
        };
        int locked = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, target, true, locked),
                charm.getHoverName()), buf -> {
            buf.writeBoolean(true);
            buf.writeVarInt(locked);
            buf.writeBoolean(false);
        });
    }

    public static void openVacuum(ServerPlayer player, AlchemicalVacuumBlockEntity vacuum, Component title) {
        FilterTarget target = new FilterTarget() {
            @Override
            public List<ItemStack> load() {
                return toStacks(vacuum.getFilter());
            }

            @Override
            public void save(List<ItemStack> filter) {
                vacuum.getFilter().clear();
                vacuum.getFilter().addAll(toIds(filter));
                vacuum.setChanged();
            }

            @Override
            public boolean stillValid(Player viewer) {
                return !vacuum.isRemoved() && viewer.distanceToSqr(Vec3.atCenterOf(vacuum.getBlockPos())) < 64;
            }
        };
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, target, false, -1), title), buf -> {
            buf.writeBoolean(false);
            buf.writeVarInt(-1);
            buf.writeBoolean(false);
        });
    }

    /**
     * Autostock: ghost count = how many of that item to keep in the inventory.
     */
    public static void openAutostock(ServerPlayer player) {
        FilterTarget target = new FilterTarget() {
            @Override
            public List<ItemStack> load() {
                List<ItemStack> result = new ArrayList<>();
                for (java.util.Map.Entry<ResourceLocation, Integer> entry : com.gabrielflorianoo.peqol.util.AutoStock.get(player).entrySet()) {
                    Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
                    if (item != null) {
                        result.add(new ItemStack(item, Math.max(1, Math.min(64, entry.getValue()))));
                    }
                }
                return result;
            }

            @Override
            public void save(List<ItemStack> filter) {
                com.gabrielflorianoo.peqol.util.AutoStock.clear(player);
                for (ItemStack stack : filter) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (id != null) {
                        com.gabrielflorianoo.peqol.util.AutoStock.set(player, id, stack.getCount());
                    }
                }
            }

            @Override
            public boolean stillValid(Player viewer) {
                return viewer == player;
            }
        };
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, target, false, -1, true),
                Component.translatable("gui.peqol.autostock")), buf -> {
            buf.writeBoolean(false);
            buf.writeVarInt(-1);
            buf.writeBoolean(true);
        });
    }

    /**
     * Exporter: ghost count = "keep N in stock", a single item = unlimited.
     */
    public static void openExporter(ServerPlayer player, com.gabrielflorianoo.peqol.block.entity.AlchemicalExporterBlockEntity exporter, Component title) {
        FilterTarget target = new FilterTarget() {
            @Override
            public List<ItemStack> load() {
                return exporter.filterStacks();
            }

            @Override
            public void save(List<ItemStack> filter) {
                exporter.setFilterStacks(filter);
            }

            @Override
            public boolean stillValid(Player viewer) {
                return !exporter.isRemoved() && viewer.distanceToSqr(Vec3.atCenterOf(exporter.getBlockPos())) < 64;
            }
        };
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, target, false, -1, true), title), buf -> {
            buf.writeBoolean(false);
            buf.writeVarInt(-1);
            buf.writeBoolean(true);
        });
    }
}
