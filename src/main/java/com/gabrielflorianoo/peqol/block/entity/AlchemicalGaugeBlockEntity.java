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
        int newSignal = signalFor(gauge.lastEmc);
        if (newSignal != gauge.signal) {
            gauge.signal = newSignal;
            gauge.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.gauge.emc", EmcUtil.format(lastEmc)).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.peqol.gauge.signal", signal).withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("info.peqol.gauge.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putInt("Signal", signal);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        signal = tag.getInt("Signal");
    }
}
