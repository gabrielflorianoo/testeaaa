package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lets players share transmutation knowledge.
 * <ul>
 *     <li>Right click a blank scroll: records everything you know.</li>
 *     <li>Right click a written scroll: learns every recorded item you don't know yet.</li>
 *     <li>Sneak + right click a written scroll: erases it.</li>
 * </ul>
 */
public class KnowledgeScrollItem extends Item {

    public static final String TAG_ENTRIES = "Knowledge";
    public static final String TAG_AUTHOR = "Author";
    public static final String TAG_COUNT = "Count";

    public KnowledgeScrollItem(Properties properties) {
        super(properties);
    }

    public static boolean isWritten(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ENTRIES, Tag.TAG_LIST);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!PEQoLConfig.COMMON.knowledgeScrollEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.peqol.disabled").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return InteractionResultHolder.fail(stack);
        }
        if (!isWritten(stack)) {
            record(serverPlayer, provider, stack);
        } else if (player.isShiftKeyDown()) {
            stack.setTag(null);
            player.displayClientMessage(Component.translatable("message.peqol.scroll.erased").withStyle(ChatFormatting.YELLOW), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 0.6F);
        } else {
            teach(serverPlayer, provider, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    private static void record(ServerPlayer player, IKnowledgeProvider provider, ItemStack stack) {
        int max = PEQoLConfig.COMMON.knowledgeScrollMaxEntries.get();
        ListTag list = new ListTag();
        for (ItemInfo info : provider.getKnowledge()) {
            if (list.size() >= max) {
                break;
            }
            list.add(info.write(new CompoundTag()));
        }
        if (list.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.peqol.scroll.nothing_known").withStyle(ChatFormatting.RED), true);
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_ENTRIES, list);
        tag.putString(TAG_AUTHOR, player.getGameProfile().getName());
        tag.putInt(TAG_COUNT, list.size());
        player.displayClientMessage(Component.translatable("message.peqol.scroll.recorded", list.size()).withStyle(ChatFormatting.GREEN), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1F, 1F);
    }

    private static void teach(ServerPlayer player, IKnowledgeProvider provider, ItemStack stack) {
        ListTag list = stack.getOrCreateTag().getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        int learned = 0;
        for (int i = 0; i < list.size(); i++) {
            ItemInfo info = ItemInfo.read(list.getCompound(i));
            if (info == null || !EmcUtil.emc().hasValue(info)) {
                continue;
            }
            ItemInfo cleaned = EmcUtil.emc().getPersistentInfo(info);
            if (provider.hasKnowledge(cleaned) || MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, info, cleaned))) {
                continue;
            }
            if (provider.addKnowledge(cleaned)) {
                learned++;
            }
        }
        if (learned > 0) {
            //One full sync instead of one packet per learned item
            provider.sync(player);
            player.displayClientMessage(Component.translatable("message.peqol.scroll.learned", learned).withStyle(ChatFormatting.GREEN), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1F, 1F);
        } else {
            player.displayClientMessage(Component.translatable("message.peqol.scroll.nothing_new").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isWritten(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (isWritten(stack) && tag != null) {
            tooltip.add(Component.translatable("tooltip.peqol.scroll.author", tag.getString(TAG_AUTHOR)).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.peqol.scroll.count", tag.getInt(TAG_COUNT)).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("item.peqol.knowledge_scroll.desc_written").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.peqol.knowledge_scroll.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
