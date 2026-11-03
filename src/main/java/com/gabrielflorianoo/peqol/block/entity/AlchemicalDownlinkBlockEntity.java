package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.block_entity.BaseEmcBlockEntity;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The reverse of the Uplink: draws the owner's personal EMC and pushes it into adjacent EMC acceptors
 * (Anti-Matter Relays, Energy Collectors, anything with ProjectE's EMC storage capability).
 */
public class AlchemicalDownlinkBlockEntity extends BaseEmcBlockEntity implements IOwnedMachine {

    private static final int INTERVAL = 10;

    private final OwnerData ownerData = new OwnerData();
    private BigInteger totalSent = BigInteger.ZERO;
    private long lastSecondSent;
    private long sentThisSecond;
    private int ticks;

    public AlchemicalDownlinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_DOWNLINK.get(), pos, state);
        setMaximumEMC(Math.max(1, PEQoLConfig.COMMON.downlinkRate.get()));
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    @Override
    protected boolean canAcceptEmc() {
        //Only the owner's personal EMC fills this block
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalDownlinkBlockEntity downlink) {
        downlink.ticks++;
        if (downlink.ticks % 20 == 0) {
            downlink.lastSecondSent = downlink.sentThisSecond;
            downlink.sentThisSecond = 0;
        }
        if (downlink.ticks % INTERVAL != 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        //Refill the buffer from personal EMC
        long needed = downlink.getNeededEmc();
        if (needed > 0) {
            ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), downlink.ownerData.getOwner());
            if (owner != null) {
                IKnowledgeProvider provider = EmcUtil.knowledge(owner).orElse(null);
                if (provider != null) {
                    long take = provider.getEmc().min(BigInteger.valueOf(needed)).longValue();
                    if (take > 0) {
                        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(take)));
                        provider.syncEmc(owner);
                        downlink.forceInsertEmc(take, EmcAction.EXECUTE);
                    }
                }
            }
        }
        if (downlink.getStoredEmc() <= 0) {
            return;
        }
        //Push to neighbours
        List<IEmcStorage> targets = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (!level.isLoaded(neighbour)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(neighbour);
            if (be == null || be instanceof AlchemicalUplinkBlockEntity || be instanceof AlchemicalDownlinkBlockEntity) {
                continue;
            }
            be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, direction.getOpposite()).ifPresent(storage -> {
                if (storage.insertEmc(1, EmcAction.SIMULATE) > 0) {
                    targets.add(storage);
                }
            });
        }
        if (targets.isEmpty()) {
            return;
        }
        long perTarget = Math.max(1, downlink.getStoredEmc() / targets.size());
        for (IEmcStorage target : targets) {
            long offer = Math.min(perTarget, downlink.getStoredEmc());
            if (offer <= 0) {
                break;
            }
            long accepted = target.insertEmc(offer, EmcAction.EXECUTE);
            if (accepted > 0) {
                downlink.forceExtractEmc(accepted, EmcAction.EXECUTE);
                downlink.totalSent = downlink.totalSent.add(BigInteger.valueOf(accepted));
                downlink.sentThisSecond += accepted;
            }
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.uplink.buffer", EmcUtil.format(getStoredEmc()), EmcUtil.format(getMaximumEmc(), true)).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.peqol.uplink.rate", EmcUtil.format(lastSecondSent)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.uplink.total", EmcUtil.format(totalSent)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.downlink.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putString("TotalSent", totalSent.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        try {
            String sent = tag.getString("TotalSent");
            totalSent = sent.isEmpty() ? BigInteger.ZERO : new BigInteger(sent);
        } catch (NumberFormatException e) {
            totalSent = BigInteger.ZERO;
        }
    }
}
