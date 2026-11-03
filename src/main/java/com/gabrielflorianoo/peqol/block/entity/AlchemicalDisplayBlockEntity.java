package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the owner's personal EMC (and its change per second) as floating text above the block.
 * The values are synced to clients once per second.
 */
public class AlchemicalDisplayBlockEntity extends BlockEntity implements IOwnedMachine {

    private final OwnerData ownerData = new OwnerData();
    private BigInteger shownEmc = BigInteger.ZERO;
    private BigInteger lastEmc;
    private BigInteger ratePerSecond = BigInteger.ZERO;
    private int ticks;

    public AlchemicalDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_DISPLAY.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public BigInteger getShownEmc() {
        return shownEmc;
    }

    public BigInteger getRatePerSecond() {
        return ratePerSecond;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalDisplayBlockEntity display) {
        if (++display.ticks % 20 != 0 || display.ownerData.getOwner() == null) {
            return;
        }
        BigInteger emc = EmcUtil.knowledge(display.ownerData.getOwner()).getEmc();
        if (emc == null) {
            emc = BigInteger.ZERO;
        }
        boolean first = display.lastEmc == null;
        BigInteger rate = first ? BigInteger.ZERO : emc.subtract(display.lastEmc);
        display.lastEmc = emc;
        if (first || !emc.equals(display.shownEmc) || !rate.equals(display.ratePerSecond)) {
            display.shownEmc = emc;
            display.ratePerSecond = rate;
            display.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.gauge.emc", EmcUtil.format(shownEmc)).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putString("ShownEmc", shownEmc.toString());
        tag.putString("Rate", ratePerSecond.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        shownEmc = parse(tag.getString("ShownEmc"));
        ratePerSecond = parse(tag.getString("Rate"));
    }

    private static BigInteger parse(String text) {
        try {
            return text.isEmpty() ? BigInteger.ZERO : new BigInteger(text);
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
