package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the EMC-powered magic items: payment helper and a two-line tooltip.
 */
public abstract class MagicItem extends Item {

    protected MagicItem(Properties properties) {
        super(properties);
    }

    /**
     * Charges personal EMC (free in creative). Tells the player when they can't afford it.
     */
    public static boolean pay(ServerPlayer player, long cost) {
        if (cost <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        if (EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost))) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.peqol.magic.too_expensive", EmcUtil.format(cost)).withStyle(ChatFormatting.RED), true);
        return false;
    }

    /**
     * Arguments for the second tooltip line (usually costs).
     */
    protected Object[] costArgs() {
        return new Object[0];
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc2", costArgs()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
