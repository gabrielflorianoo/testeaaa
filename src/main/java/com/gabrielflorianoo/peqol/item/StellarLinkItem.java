package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, keeps Klein Stars (and any other EMC holding item) in the inventory charged from personal EMC.
 */
public class StellarLinkItem extends ToggleableItem {

    public StellarLinkItem(Properties properties) {
        super(properties);
    }

    /**
     * Called once per second.
     */
    public static void charge(ServerPlayer player) {
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return;
        }
        BigInteger emc = provider.getEmc();
        long budget = emc.min(BigInteger.valueOf(PEQoLConfig.COMMON.stellarLinkRate.get())).longValue();
        if (budget <= 0) {
            return;
        }
        long[] remaining = {budget};
        InventoryUtil.forEachStack(player, stack -> {
            if (remaining[0] > 0 && !(stack.getItem() instanceof StellarLinkItem)) {
                remaining[0] -= EmcUtil.chargeHolder(stack, remaining[0]);
            }
        });
        long spent = budget - remaining[0];
        if (spent > 0) {
            provider.setEmc(emc.subtract(BigInteger.valueOf(spent)));
            provider.syncEmc(player);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.stellar_link.desc2", EmcUtil.compact(BigInteger.valueOf(PEQoLConfig.COMMON.stellarLinkRate.get())))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
