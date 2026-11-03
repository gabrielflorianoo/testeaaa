package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, picked up items that match the filter are instantly converted into personal EMC.
 * Sneak + right click with an item in the other hand toggles that item in the filter.
 */
public class SiphonCharmItem extends ToggleableItem {

    public static final String TAG_FILTER = "Filter";

    public SiphonCharmItem(Properties properties) {
        super(properties);
    }

    public static List<ResourceLocation> getFilter(ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_FILTER, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_FILTER, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    public static boolean matches(ItemStack charm, ItemStack candidate) {
        CompoundTag tag = charm.getTag();
        if (tag == null || candidate.isEmpty() || !tag.contains(TAG_FILTER, Tag.TAG_LIST)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(candidate.getItem());
        if (id == null) {
            return false;
        }
        String key = id.toString();
        ListTag list = tag.getList(TAG_FILTER, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResultHolder<ItemStack> useSneaking(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.isEmpty()) {
            List<ResourceLocation> filter = getFilter(stack);
            if (filter.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.peqol.siphon.empty").withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(Component.translatable("message.peqol.siphon.list", filter.size()).withStyle(ChatFormatting.GOLD), false);
                for (ResourceLocation id : filter) {
                    Item item = ForgeRegistries.ITEMS.getValue(id);
                    player.displayClientMessage(Component.literal(" - ").append(item == null ? Component.literal(id.toString()) : item.getDescription()), false);
                }
            }
            return InteractionResultHolder.success(stack);
        }
        if (!EmcUtil.hasValue(other)) {
            player.displayClientMessage(Component.translatable("message.peqol.no_emc", other.getHoverName()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(other.getItem());
        if (id == null) {
            return InteractionResultHolder.fail(stack);
        }
        ListTag list = stack.getOrCreateTag().getList(TAG_FILTER, Tag.TAG_STRING);
        String key = id.toString();
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.getString(i))) {
                list.remove(i);
                stack.getOrCreateTag().put(TAG_FILTER, list);
                player.displayClientMessage(Component.translatable("message.peqol.siphon.removed", other.getHoverName()).withStyle(ChatFormatting.YELLOW), true);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 0.6F, 1F);
                return InteractionResultHolder.success(stack);
            }
        }
        int max = PEQoLConfig.COMMON.siphonMaxFilters.get();
        if (list.size() >= max) {
            player.displayClientMessage(Component.translatable("message.peqol.filter_full", max).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        list.add(StringTag.valueOf(key));
        stack.getOrCreateTag().put(TAG_FILTER, list);
        player.displayClientMessage(Component.translatable("message.peqol.siphon.added", other.getHoverName()).withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.6F, 1F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.siphon_charm.desc2").withStyle(ChatFormatting.DARK_GRAY));
        List<ResourceLocation> filter = getFilter(stack);
        if (!filter.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.peqol.filter", filter.size()).withStyle(ChatFormatting.GOLD));
            int shown = 0;
            for (ResourceLocation id : filter) {
                if (shown++ >= 8) {
                    tooltip.add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                Item item = ForgeRegistries.ITEMS.getValue(id);
                MutableComponent name = item == null ? Component.literal(id.toString()) : item.getDescription().copy();
                tooltip.add(Component.literal("  ").append(name).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
