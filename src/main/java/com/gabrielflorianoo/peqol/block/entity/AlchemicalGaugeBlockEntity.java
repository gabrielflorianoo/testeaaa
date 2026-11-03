package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Emits a comparator signal equal to the number of digits of the owner's personal EMC (capped at 15):
 * 0 EMC = 0, 1-9 = 1, 10-99 = 2, ..., 100 trillion+ = 15. Works while the owner is offline too (read only).
 */
public class AlchemicalGaugeBlockEntity extends BlockEntity implements IOwnedMachine {

    private final OwnerData ownerData = new OwnerData();
    private int signal;
    private BigInteger lastEmc = BigInteger.ZERO;
    private int ticks;
    private boolean thresholdMode;
    private BigInteger threshold = BigInteger.valueOf(1_000_000);

    public void setThreshold(BigInteger threshold) {
        this.threshold = threshold.signum() < 0 ? BigInteger.ZERO : threshold;
        thresholdMode = true;
        setChanged();
    }

    public boolean isThresholdMode() {
        return thresholdMode;
    }

    public AlchemicalGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_GAUGE.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public int getSignal() {
        return signal;
    }

    public static int signalFor(BigInteger emc) {
        if (emc.signum() <= 0) {
            return 0;
        }
        return Math.min(15, emc.toString().length());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalGaugeBlockEntity gauge) {
        if (++gauge.ticks % 20 != 0 || gauge.ownerData.getOwner() == null) {
            return;
        }
        BigInteger emc = EmcUtil.knowledge(gauge.ownerData.getOwner()).getEmc();
        gauge.lastEmc = emc == null ? BigInteger.ZERO : emc;
        int newSignal = gauge.thresholdMode ? (gauge.lastEmc.compareTo(gauge.threshold) >= 0 ? 15 : 0) : signalFor(gauge.lastEmc);
        if (newSignal != gauge.signal) {
            gauge.signal = newSignal;
            gauge.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /**
     * Sneak + empty hand: switch between "digits" and "threshold" modes.
     */
    @Override
    public void onSneakUse(net.minecraft.server.level.ServerPlayer player) {
        if (!ownerData.canConfigure(player)) {
            return;
        }
        thresholdMode = !thresholdMode;
        setChanged();
        player.displayClientMessage(thresholdMode ? Component.translatable("message.peqol.gauge.threshold", EmcUtil.format(threshold))
                : Component.translatable("message.peqol.gauge.digits"), true);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.gauge.emc", EmcUtil.format(lastEmc)).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.peqol.gauge.signal", signal).withStyle(ChatFormatting.RED));
        if (thresholdMode) {
            lines.add(Component.translatable("message.peqol.gauge.threshold", EmcUtil.format(threshold)).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("info.peqol.gauge.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putInt("Signal", signal);
        tag.putBoolean("ThresholdMode", thresholdMode);
        tag.putString("Threshold", threshold.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        signal = tag.getInt("Signal");
        thresholdMode = tag.getBoolean("ThresholdMode");
        try {
            String text = tag.getString("Threshold");
            threshold = text.isEmpty() ? BigInteger.valueOf(1_000_000) : new BigInteger(text);
        } catch (NumberFormatException e) {
            threshold = BigInteger.valueOf(1_000_000);
        }
    }
}
