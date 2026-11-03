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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, repairs damaged gear in the inventory using personal EMC.
 */
public class MendingCharmItem extends ToggleableItem {

    public MendingCharmItem(Properties properties) {
        super(properties);
    }

    /**
     * @return EMC needed to restore one point of durability, 0 if the item can't be repaired this way.
     */
    public static long costPerDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 0;
        }
        long value = EmcUtil.value(new ItemStack(stack.getItem()));
        if (value <= 0) {
            return 0;
        }
        double multiplier = PEQoLConfig.COMMON.mendingCostMultiplier.get();
        return Math.max(1L, (long) Math.ceil(value * multiplier / stack.getMaxDamage()));
    }

    public static void mend(ServerPlayer player) {
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return;
        }
        int maxPoints = PEQoLConfig.COMMON.mendingPointsPerCycle.get();
        Inventory inventory = player.getInventory();
        ItemStack using = player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY;
        BigInteger emc = provider.getEmc();
        boolean changed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.isDamaged() || stack == using) {
                continue;
            }
            long unit = costPerDurability(stack);
            if (unit <= 0) {
                continue;
            }
            int points = (int) Math.min(Math.min(stack.getDamageValue(), maxPoints), EmcUtil.affordable(emc, unit));
            if (points <= 0) {
                continue;
            }
            stack.setDamageValue(stack.getDamageValue() - points);
            emc = emc.subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(points)));
            changed = true;
        }
        if (changed) {
            provider.setEmc(emc);
            provider.syncEmc(player);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.mending_charm.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
