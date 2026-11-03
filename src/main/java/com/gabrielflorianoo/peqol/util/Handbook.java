package com.gabrielflorianoo.peqol.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Alchemist's Handbook: a written book explaining every QoL+ feature.
 * Pages are translatable components, so they show in each player's own language.
 */
public final class Handbook {

    public static final int PAGES = 14;

    private Handbook() {
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "ProjectE QoL+");
        tag.putString("author", "The Alchemist");
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        for (int i = 1; i <= PAGES; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.peqol.page" + i))));
        }
        tag.put("pages", pages);
        CompoundTag display = new CompoundTag();
        display.putString("Name", Component.Serializer.toJson(Component.translatable("book.peqol.title")));
        tag.put("display", display);
        return book;
    }
}
