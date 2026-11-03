package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * Exposes the owner's transmutation knowledge as an item handler: every learned item is a "slot" whose count is how many
 * the owner can afford. Extracting creates the item from personal EMC, inserting burns it. Works with hoppers, pipes and
 * storage mods (AE2 storage bus, RS external storage...). Only active while the owner is online.
 */
public class AlchemicalInterfaceBlockEntity extends BlockEntity implements IOwnedMachine {

    private static final int REFRESH_TICKS = 40;

    private final OwnerData ownerData = new OwnerData();
    private final InterfaceHandler handler = new InterfaceHandler();
    private LazyOptional<IItemHandler> handlerCap = LazyOptional.of(() -> handler);
    private List<ItemStack> templates = new ArrayList<>();
    private int ticks;
    private long itemsOut;
    private long itemsIn;

    public AlchemicalInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_INTERFACE.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalInterfaceBlockEntity be) {
        if (be.ticks++ % REFRESH_TICKS == 0) {
            be.refresh();
        }
    }

    @Nullable
    private ServerPlayer owner() {
        return level == null ? null : EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
    }

    /**
     * Rebuilds the list of exposed items from the owner's knowledge (capped, sorted by id for stable slots).
     */
    public void refresh() {
        List<ItemStack> result = new ArrayList<>();
        ServerPlayer owner = owner();
        IKnowledgeProvider provider = owner == null ? null : EmcUtil.knowledge(owner).orElse(null);
        if (provider != null) {
            int max = PEQoLConfig.COMMON.interfaceMaxSlots.get();
            for (ItemInfo info : provider.getKnowledge()) {
                if (result.size() >= max) {
                    break;
                }
                ItemStack stack = info.createStack();
                if (!stack.isEmpty() && EmcUtil.hasValue(stack) && !EmcUtil.isBlacklisted(stack)) {
                    result.add(stack);
                }
            }
            result.sort((a, b) -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(a.getItem()))
                    .compareTo(String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(b.getItem()))));
        }
        templates = result;
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(owner() == null ? "info.peqol.interface.offline" : "info.peqol.interface.online", templates.size())
                .withStyle(owner() == null ? ChatFormatting.RED : ChatFormatting.GREEN));
        lines.add(Component.translatable("info.peqol.interface.stats", EmcUtil.format(itemsOut), EmcUtil.format(itemsIn)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.interface.hint").withStyle(ChatFormatting.DARK_GRAY));
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
        tag.putLong("ItemsOut", itemsOut);
        tag.putLong("ItemsIn", itemsIn);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        itemsOut = tag.getLong("ItemsOut");
        itemsIn = tag.getLong("ItemsIn");
    }

    private class InterfaceHandler implements IItemHandler {

        @Override
        public int getSlots() {
            //One extra, always empty slot so inserting always has a target
            return templates.size() + 1;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= templates.size()) {
                return ItemStack.EMPTY;
            }
            ServerPlayer owner = owner();
            if (owner == null) {
                return ItemStack.EMPTY;
            }
            ItemStack template = templates.get(slot);
            long affordable = EmcUtil.affordable(EmcUtil.getEmc(owner), EmcUtil.value(template));
            int count = (int) Math.min(affordable, template.getMaxStackSize());
            return count <= 0 ? ItemStack.EMPTY : template.copyWithCount(count);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            ServerPlayer owner = owner();
            if (owner == null || stack.isEmpty() || EmcUtil.isEmcHolder(stack) || EmcUtil.sellValue(stack) <= 0) {
                return stack;
            }
            if (!simulate) {
                EmcUtil.burn(owner, stack, PEQoLConfig.COMMON.importerLearnsItems.get());
                itemsIn += stack.getCount();
                setChanged();
            }
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= templates.size() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ServerPlayer owner = owner();
            IKnowledgeProvider provider = owner == null ? null : EmcUtil.knowledge(owner).orElse(null);
            if (provider == null) {
                return ItemStack.EMPTY;
            }
            ItemStack template = templates.get(slot);
            if (!provider.hasKnowledge(template)) {
                return ItemStack.EMPTY;
            }
            long unit = EmcUtil.value(template);
            int count = (int) Math.min(Math.min(amount, template.getMaxStackSize()), EmcUtil.affordable(provider.getEmc(), unit));
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count))));
                provider.syncEmc(owner);
                itemsOut += count;
                setChanged();
            }
            return template.copyWithCount(count);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return EmcUtil.hasValue(stack) && !EmcUtil.isEmcHolder(stack);
        }
    }
}
