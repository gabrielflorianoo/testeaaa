package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.block_entity.BaseEmcBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Accepts EMC from Energy Collectors / Anti-Matter Relays and deposits it straight into the owner's personal EMC.
 * Buffers while the owner is offline.
 */
public class AlchemicalUplinkBlockEntity extends BaseEmcBlockEntity implements IOwnedMachine {

    private static final int FLUSH_INTERVAL = 10;

    private final OwnerData ownerData = new OwnerData();
    private BigInteger totalTransferred = BigInteger.ZERO;
    private long lastSecondTransferred;
    private long transferredThisSecond;
    private int ticks;
    private int lastSignal;

    public AlchemicalUplinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_UPLINK.get(), pos, state);
        setMaximumEMC(PEQoLConfig.COMMON.uplinkBufferCapacity.get());
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    @Override
    protected boolean canProvideEmc() {
        //Only the owner's personal EMC drains this block
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalUplinkBlockEntity uplink) {
        uplink.ticks++;
        if (uplink.ticks % 20 == 0) {
            uplink.lastSecondTransferred = uplink.transferredThisSecond;
            uplink.transferredThisSecond = 0;
        }
        if (uplink.ticks % FLUSH_INTERVAL != 0 || uplink.getStoredEmc() <= 0) {
            return;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), uplink.ownerData.getOwner());
        if (owner == null) {
            return;
        }
        long amount = uplink.forceExtractEmc(uplink.getStoredEmc(), EmcAction.EXECUTE);
        if (amount > 0) {
            EmcUtil.addEmc(owner, BigInteger.valueOf(amount));
            uplink.totalTransferred = uplink.totalTransferred.add(BigInteger.valueOf(amount));
            uplink.transferredThisSecond += amount;
        }
    }

    /**
     * @return 0-15 comparator signal based on how full the buffer is.
     */
    public int getComparatorSignal() {
        if (getStoredEmc() <= 0) {
            return 0;
        }
        return 1 + (int) Math.min(14, getStoredEmc() * 14.0 / getMaximumEmc());
    }

    @Override
    protected void storedEmcChanged() {
        int before = lastSignal;
        super.storedEmcChanged();
        lastSignal = getComparatorSignal();
        if (before != lastSignal && level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.uplink.buffer", EmcUtil.format(getStoredEmc()), EmcUtil.format(getMaximumEmc(), true)).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.peqol.uplink.rate", EmcUtil.format(lastSecondTransferred)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.uplink.total", EmcUtil.format(totalTransferred)).withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putString("TotalTransferred", totalTransferred.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        try {
            totalTransferred = new BigInteger(tag.getString("TotalTransferred"));
        } catch (NumberFormatException e) {
            totalTransferred = BigInteger.ZERO;
        }
    }
}
