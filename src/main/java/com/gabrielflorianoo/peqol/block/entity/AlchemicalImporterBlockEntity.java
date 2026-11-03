package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hopper/pipe friendly "transmutation table": every item inserted is converted into the owner's personal EMC (and learned).
 * Items without EMC are rejected so they stay in the source inventory.
 */
public class AlchemicalImporterBlockEntity extends BlockEntity implements IOwnedMachine {

    private static final int MAX_PENDING_LEARN = 1024;

    private final OwnerData ownerData = new OwnerData();
    private final ImportHandler handler = new ImportHandler();
    private LazyOptional<IItemHandler> handlerCap = LazyOptional.of(() -> handler);
    /**
     * EMC gathered while the owner was offline.
     */
    private BigInteger pendingEmc = BigInteger.ZERO;
    private final Set<ItemInfo> pendingLearn = new LinkedHashSet<>();
    private BigInteger totalImported = BigInteger.ZERO;
    private long itemsImported;
    private int ticks;

    public AlchemicalImporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_IMPORTER.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalImporterBlockEntity importer) {
        if (++importer.ticks % 20 != 0 || (importer.pendingEmc.signum() == 0 && importer.pendingLearn.isEmpty())) {
            return;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), importer.ownerData.getOwner());
        if (owner != null) {
            importer.flush(owner);
        }
    }

    private void flush(ServerPlayer owner) {
        if (pendingEmc.signum() > 0) {
            EmcUtil.addEmc(owner, pendingEmc);
            pendingEmc = BigInteger.ZERO;
        }
        if (!pendingLearn.isEmpty()) {
            for (ItemInfo info : pendingLearn) {
                EmcUtil.tryLearn(owner, info.createStack());
            }
            pendingLearn.clear();
        }
        setChanged();
    }

    /**
     * @return true if the stack was accepted.
     */
    public BigInteger getPendingEmc() {
        return pendingEmc;
    }

    public boolean importStack(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !ownerData.hasOwner() || level == null || level.isClientSide) {
            return false;
        }
        BigInteger value = EmcUtil.stackSellValue(stack);
        if (value.signum() <= 0) {
            return false;
        }
        if (simulate) {
            return true;
        }
        boolean learn = PEQoLConfig.COMMON.importerLearnsItems.get();
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        if (owner != null) {
            if (learn) {
                EmcUtil.tryLearn(owner, stack);
            }
            EmcUtil.addEmc(owner, value);
        } else {
            pendingEmc = pendingEmc.add(value);
            if (learn && pendingLearn.size() < MAX_PENDING_LEARN) {
                pendingLearn.add(ItemInfo.fromStack(stack.copyWithCount(1)));
            }
        }
        totalImported = totalImported.add(value);
        itemsImported += stack.getCount();
        setChanged();
        return true;
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.importer.total", EmcUtil.format(itemsImported), EmcUtil.format(totalImported)).withStyle(ChatFormatting.GOLD));
        if (pendingEmc.signum() > 0) {
            lines.add(Component.translatable("info.peqol.importer.pending", EmcUtil.format(pendingEmc)).withStyle(ChatFormatting.YELLOW));
        }
        lines.add(Component.translatable("info.peqol.importer.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handlerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        handlerCap = LazyOptional.of(() -> handler);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putString("PendingEmc", pendingEmc.toString());
        tag.putString("TotalImported", totalImported.toString());
        tag.putLong("ItemsImported", itemsImported);
        ListTag learn = new ListTag();
        for (ItemInfo info : pendingLearn) {
            learn.add(info.write(new CompoundTag()));
        }
        tag.put("PendingLearn", learn);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        pendingEmc = parse(tag.getString("PendingEmc"));
        totalImported = parse(tag.getString("TotalImported"));
        itemsImported = tag.getLong("ItemsImported");
        pendingLearn.clear();
        ListTag learn = tag.getList("PendingLearn", Tag.TAG_COMPOUND);
        for (int i = 0; i < learn.size(); i++) {
            ItemInfo info = ItemInfo.read(learn.getCompound(i));
            if (info != null) {
                pendingLearn.add(info);
            }
        }
    }

    private static BigInteger parse(String text) {
        try {
            return text.isEmpty() ? BigInteger.ZERO : new BigInteger(text);
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }

    private class ImportHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return importStack(stack, simulate) ? ItemStack.EMPTY : stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return EmcUtil.hasValue(stack);
        }
    }
}
