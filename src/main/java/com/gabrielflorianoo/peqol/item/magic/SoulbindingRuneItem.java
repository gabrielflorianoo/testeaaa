package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Soulbinding Rune: hold the rune and an item in the other hand, right click to bind the item to your soul
 * (sneak to release it). Soulbound items stay with you when you die.
 */
public class SoulbindingRuneItem extends MagicItem {

    public static final String TAG_SOULBOUND = "PEQoLSoulbound";
    public static final String DATA_KEPT = "peqol_soulbound_items";

    public SoulbindingRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.soulbindCost.get())};
    }

    public static boolean isSoulbound(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_SOULBOUND);
    }

    public static void setSoulbound(ItemStack stack, boolean bound) {
        if (bound) {
            stack.getOrCreateTag().putBoolean(TAG_SOULBOUND, true);
        } else if (stack.hasTag()) {
            stack.getTag().remove(TAG_SOULBOUND);
            if (stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    /**
     * Pulls soulbound items out of the death drops and remembers them on the dead player.
     */
    public static int keepFromDrops(Player player, Collection<ItemEntity> drops) {
        ListTag kept = player.getPersistentData().getList(DATA_KEPT, Tag.TAG_COMPOUND);
        int[] count = {0};
        drops.removeIf(entity -> {
            ItemStack stack = entity.getItem();
            if (isSoulbound(stack)) {
                kept.add(stack.save(new CompoundTag()));
                count[0]++;
                return true;
            }
            return false;
        });
        if (count[0] > 0) {
            player.getPersistentData().put(DATA_KEPT, kept);
        }
        return count[0];
    }

    /**
     * Gives the remembered items to the respawned player.
     */
    public static void restore(Player original, Player respawned) {
        ListTag kept = original.getPersistentData().getList(DATA_KEPT, Tag.TAG_COMPOUND);
        if (kept.isEmpty()) {
            return;
        }
        for (int i = 0; i < kept.size(); i++) {
            ItemStack stack = ItemStack.of(kept.getCompound(i));
            if (!respawned.getInventory().add(stack)) {
                respawned.drop(stack, false);
            }
        }
        original.getPersistentData().remove(DATA_KEPT);
        respawned.getPersistentData().remove(DATA_KEPT);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack rune = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(rune);
        }
        ItemStack target = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (target.isEmpty() || target.getItem() instanceof SoulbindingRuneItem) {
            player.displayClientMessage(Component.translatable("message.peqol.soulbind.hint").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(rune);
        }
        if (player.isShiftKeyDown()) {
            if (isSoulbound(target)) {
                setSoulbound(target, false);
                player.displayClientMessage(Component.translatable("message.peqol.soulbind.released", target.getHoverName()).withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1F, 1.4F);
            }
            return InteractionResultHolder.success(rune);
        }
        if (isSoulbound(target)) {
            player.displayClientMessage(Component.translatable("message.peqol.soulbind.already", target.getHoverName()).withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(rune);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.soulbindCost.get())) {
            return InteractionResultHolder.fail(rune);
        }
        setSoulbound(target, true);
        player.displayClientMessage(Component.translatable("message.peqol.soulbind.bound", target.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        ((ServerLevel) level).sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 25, 0.4, 0.5, 0.4, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1F, 1.2F);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(rune);
    }
}
