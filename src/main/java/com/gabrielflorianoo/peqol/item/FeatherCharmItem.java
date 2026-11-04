package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, fall damage is cancelled, paying personal EMC per block fallen.
 */
public class FeatherCharmItem extends ToggleableItem {

    public FeatherCharmItem(Properties properties) {
        super(properties);
    }

    public static long costFor(float distance) {
        float blocks = Math.max(0, distance - 3);
        return (long) Math.ceil(blocks) * PEQoLConfig.COMMON.featherCostPerBlock.get();
    }

    /**
     * @return true if the fall damage should be cancelled.
     */
    public static boolean absorb(ServerPlayer player, float distance) {
        long cost = player.getAbilities().instabuild ? 0 : costFor(distance);
        return cost <= 0 || EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.feather_charm.desc2", EmcUtil.format(PEQoLConfig.COMMON.featherCostPerBlock.get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
