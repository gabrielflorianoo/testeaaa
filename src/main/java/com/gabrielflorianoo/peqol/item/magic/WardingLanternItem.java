package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Warding Lantern: while active and carried, hostile mobs can't spawn naturally around you. Paid per minute.
 */
public class WardingLanternItem extends ToggleableItem {

    public static final int INTERVAL = 100;
    private static final Set<UUID> WARDED = ConcurrentHashMap.newKeySet();

    public WardingLanternItem(Properties properties) {
        super(properties);
    }

    public static long costPerInterval() {
        return (PEQoLConfig.COMMON.wardCostPerMinute.get() * INTERVAL + 1199) / 1200;
    }

    /**
     * Called every {@link #INTERVAL} ticks: pays and refreshes the ward of a player carrying an active lantern.
     */
    public static void refresh(ServerPlayer player, boolean carrying) {
        long cost = costPerInterval();
        if (carrying && (player.getAbilities().instabuild || cost <= 0 || EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost)))) {
            WARDED.add(player.getUUID());
        } else {
            WARDED.remove(player.getUUID());
        }
    }

    public static void forget(UUID player) {
        WARDED.remove(player);
    }

    /**
     * Whether a warded player is close enough to {@code (x, y, z)} to stop a spawn there.
     */
    public static boolean isWarded(Level level, double x, double y, double z) {
        if (WARDED.isEmpty()) {
            return false;
        }
        double radius = PEQoLConfig.COMMON.wardRadius.get();
        for (Player player : level.players()) {
            if (WARDED.contains(player.getUUID()) && player.distanceToSqr(x, y, z) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.peqol.warding_lantern.desc2", PEQoLConfig.COMMON.wardRadius.get(),
                EmcUtil.format(PEQoLConfig.COMMON.wardCostPerMinute.get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
