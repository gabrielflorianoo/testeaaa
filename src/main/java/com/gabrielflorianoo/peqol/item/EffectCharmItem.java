package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, keeps a potion effect on the player, paid with personal EMC per minute.
 */
public class EffectCharmItem extends ToggleableItem {

    /**
     * Ticks between payments / effect refreshes.
     */
    public static final int INTERVAL = 100;
    private static final int DURATION = 320;

    private final Supplier<MobEffect> effect;
    private final int amplifier;
    private final Supplier<ForgeConfigSpec.LongValue> costPerMinute;

    public EffectCharmItem(Properties properties, Supplier<MobEffect> effect, int amplifier, Supplier<ForgeConfigSpec.LongValue> costPerMinute) {
        super(properties);
        this.effect = effect;
        this.amplifier = amplifier;
        this.costPerMinute = costPerMinute;
    }

    public MobEffect getEffect() {
        return effect.get();
    }

    public long costPerInterval() {
        long perMinute = costPerMinute.get().get();
        //1200 ticks per minute
        return perMinute <= 0 ? 0 : Math.max(1, (perMinute * INTERVAL + 1199) / 1200);
    }

    /**
     * Pays for and refreshes the effect.
     *
     * @return true if the effect was applied.
     */
    public boolean apply(ServerPlayer player) {
        long cost = player.getAbilities().instabuild ? 0 : costPerInterval();
        if (cost > 0 && !EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(cost))) {
            return false;
        }
        player.addEffect(new MobEffectInstance(effect.get(), DURATION, amplifier, true, false, true));
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.peqol.effect_charm.cost", EmcUtil.format(costPerMinute.get().get())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
