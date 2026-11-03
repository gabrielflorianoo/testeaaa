package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click any inventory (chest, barrel, drawer, alchemical chest...) to see the EMC value of its contents,
 * or an EMC storage block (collector, relay, condenser...) to see how much EMC it stores.
 * Right click in the air to appraise your own inventory.
 */
public class AppraisalLensItem extends Item {

    private static final int TOP_ENTRIES = 5;

    public AppraisalLensItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (player == null || be == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean any = false;
        Component name = level.getBlockState(context.getClickedPos()).getBlock().getName();
        player.sendSystemMessage(Component.translatable("message.peqol.lens.header", name).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        IEmcStorage storage = be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, context.getClickedFace()).resolve().orElse(null);
        if (storage != null) {
            any = true;
            player.sendSystemMessage(Component.translatable("message.peqol.lens.storage", EmcUtil.format(storage.getStoredEmc()),
                    EmcUtil.format(storage.getMaximumEmc(), true)).withStyle(ChatFormatting.YELLOW));
        }
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, context.getClickedFace()).resolve().orElse(null);
        if (handler == null) {
            handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        }
        if (handler != null) {
            any = true;
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                stacks.add(handler.getStackInSlot(i));
            }
            report(player, stacks);
        }
        if (!any) {
            player.sendSystemMessage(Component.translatable("message.peqol.lens.nothing").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide) {
            player.sendSystemMessage(Component.translatable("message.peqol.lens.self").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            Inventory inventory = player.getInventory();
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                stacks.add(inventory.getItem(i));
            }
            report(player, stacks);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    private static void report(Player player, List<ItemStack> stacks) {
        BigInteger total = BigInteger.ZERO;
        int items = 0;
        int noEmc = 0;
        List<ItemStack> valued = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            items += stack.getCount();
            BigInteger value = EmcUtil.stackValue(stack);
            if (value.signum() > 0) {
                total = total.add(value);
                valued.add(stack);
            } else {
                noEmc += stack.getCount();
            }
        }
        player.sendSystemMessage(Component.translatable("message.peqol.lens.total", Component.literal(EmcUtil.format(total)).withStyle(ChatFormatting.YELLOW),
                items).withStyle(ChatFormatting.GOLD));
        if (noEmc > 0) {
            player.sendSystemMessage(Component.translatable("tooltip.peqol.contents_no_emc", noEmc).withStyle(ChatFormatting.DARK_GRAY));
        }
        valued.sort((a, b) -> EmcUtil.stackValue(b).compareTo(EmcUtil.stackValue(a)));
        for (int i = 0; i < Math.min(TOP_ENTRIES, valued.size()); i++) {
            ItemStack stack = valued.get(i);
            player.sendSystemMessage(Component.literal(" " + (i + 1) + ". " + stack.getCount() + "x ").append(stack.getHoverName())
                    .append(Component.literal(" - " + EmcUtil.compact(EmcUtil.stackValue(stack)) + " EMC").withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.appraisal_lens.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.appraisal_lens.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
