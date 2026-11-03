package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active and somewhere in the inventory, tops up hotbar stacks from personal EMC.
 */
public class RestockerTalismanItem extends ToggleableItem {

    public RestockerTalismanItem(Properties properties) {
        super(properties);
    }

    /**
     * Called every {@link PEQoLConfig.Common#restockerInterval} ticks for players that carry an active talisman.
     */
    public static void restock(ServerPlayer player) {
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        int threshold = PEQoLConfig.COMMON.restockerThreshold.get();
        boolean blocksOnly = PEQoLConfig.COMMON.restockerBlocksOnly.get();
        boolean changed = false;
        BigInteger emc = provider.getEmc();

        int hotbarSize = Inventory.getSelectionSize();
        for (int slot = 0; slot <= hotbarSize; slot++) {
            ItemStack stack;
            if (slot == hotbarSize) {
                stack = inventory.offhand.get(0);
            } else {
                if (!PEQoLConfig.COMMON.restockerWholeHotbar.get() && slot != inventory.selected) {
                    continue;
                }
                stack = inventory.items.get(slot);
            }
            if (stack.isEmpty() || stack.hasTag() || !stack.isStackable() || stack.getCount() > threshold) {
                continue;
            }
            if (blocksOnly && !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            int missing = stack.getMaxStackSize() - stack.getCount();
            if (missing <= 0 || !provider.hasKnowledge(stack)) {
                continue;
            }
            long unit = EmcUtil.value(stack);
            if (unit <= 0) {
                continue;
            }
            int toAdd = (int) Math.min(missing, EmcUtil.affordable(emc, unit));
            if (toAdd <= 0) {
                continue;
            }
            emc = emc.subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(toAdd)));
            stack.grow(toAdd);
            changed = true;
        }
        if (changed) {
            provider.setEmc(emc);
            provider.syncEmc(player);
            inventory.setChanged();
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.restocker_talisman.desc2", PEQoLConfig.COMMON.restockerThreshold.get()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
