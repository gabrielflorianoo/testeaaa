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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the filtered items out of the owner's personal EMC and pushes them into adjacent inventories.
 * Only works while the owner is online and only for items the owner has learned. A redstone signal pauses it.
 */
public class AlchemicalExporterBlockEntity extends BlockEntity implements IOwnedMachine, IUpgradeable {

    private final OwnerData ownerData = new OwnerData();
    private final List<ItemInfo> filters = new ArrayList<>();
    /**
     * Parallel to {@link #filters}: how many items to keep in the adjacent inventories, 0 = unlimited.
     */
    private final List<Integer> stockTargets = new ArrayList<>();
    private BigInteger totalSpent = BigInteger.ZERO;
    private long itemsExported;
    private int ticks;
    private Status status = Status.IDLE;
    private int tier;

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public void setTier(int tier) {
        this.tier = Math.max(0, Math.min(MAX_TIER, tier));
        setChanged();
    }

    public enum Status {
        IDLE, RUNNING, NO_OWNER, OWNER_OFFLINE, NO_EMC, NO_TARGET, PAUSED
    }

    public AlchemicalExporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_EXPORTER.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public List<ItemInfo> getFilters() {
        return filters;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalExporterBlockEntity exporter) {
        if (++exporter.ticks % Math.max(1, PEQoLConfig.COMMON.exporterInterval.get() / (1 + exporter.tier)) != 0) {
            return;
        }
        exporter.status = exporter.operate(level, pos);
    }

    private Status operate(Level level, BlockPos pos) {
        if (filters.isEmpty()) {
            return Status.IDLE;
        }
        if (level.hasNeighborSignal(pos)) {
            return Status.PAUSED;
        }
        if (!ownerData.hasOwner()) {
            return Status.NO_OWNER;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        if (owner == null) {
            return Status.OWNER_OFFLINE;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(owner).orElse(null);
        if (provider == null) {
            return Status.OWNER_OFFLINE;
        }
        List<IItemHandler> targets = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            if (!level.isLoaded(targetPos)) {
                continue;
            }
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null || target instanceof AlchemicalExporterBlockEntity || target instanceof AlchemicalImporterBlockEntity) {
                continue;
            }
            target.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(targets::add);
        }
        if (targets.isEmpty()) {
            return Status.NO_TARGET;
        }
        int perOperation = Math.min(64, PEQoLConfig.COMMON.exporterItemsPerOperation.get() * (1 + tier));
        BigInteger emc = provider.getEmc();
        BigInteger startEmc = emc;
        boolean anyEmcShortage = false;
        for (int index = 0; index < filters.size(); index++) {
            ItemInfo filter = filters.get(index);
            long unit = EmcUtil.emc().getValue(filter);
            if (unit <= 0 || !provider.hasKnowledge(filter) || EmcUtil.isBlacklisted(filter.createStack())) {
                continue;
            }
            ItemStack template = filter.createStack();
            int limit = Math.min(perOperation, template.getMaxStackSize());
            int stock = stockTargets.get(index);
            if (stock > 0) {
                int present = countIn(targets, template);
                if (present >= stock) {
                    continue;
                }
                limit = Math.min(limit, stock - present);
            }
            int amount = (int) Math.min(limit, EmcUtil.affordable(emc, unit));
            if (amount <= 0) {
                anyEmcShortage = true;
                continue;
            }
            for (IItemHandler target : targets) {
                if (amount <= 0) {
                    break;
                }
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, template.copyWithCount(amount), true);
                int accepted = amount - remainder.getCount();
                if (accepted <= 0) {
                    continue;
                }
                remainder = ItemHandlerHelper.insertItemStacked(target, template.copyWithCount(accepted), false);
                int inserted = accepted - remainder.getCount();
                if (inserted > 0) {
                    BigInteger cost = BigInteger.valueOf(unit).multiply(BigInteger.valueOf(inserted));
                    emc = emc.subtract(cost);
                    totalSpent = totalSpent.add(cost);
                    itemsExported += inserted;
                    amount -= inserted;
                }
            }
        }
        if (!emc.equals(startEmc)) {
            provider.setEmc(emc);
            provider.syncEmc(owner);
            setChanged();
            return Status.RUNNING;
        }
        return anyEmcShortage ? Status.NO_EMC : Status.IDLE;
    }

    private static int countIn(List<IItemHandler> targets, ItemStack template) {
        int count = 0;
        for (IItemHandler target : targets) {
            for (int slot = 0; slot < target.getSlots(); slot++) {
                ItemStack stack = target.getStackInSlot(slot);
                if (ItemHandlerHelper.canItemStacksStack(stack, template)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    @Override
    public boolean onItemUse(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (!ownerData.canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", ownerData.describe()).withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!EmcUtil.hasValue(held)) {
            player.displayClientMessage(Component.translatable("message.peqol.no_emc", held.getHoverName()).withStyle(ChatFormatting.RED), true);
            return true;
        }
        ItemInfo info = EmcUtil.emc().getPersistentInfo(ItemInfo.fromStack(held));
        int existing = filters.indexOf(info);
        if (player.isShiftKeyDown()) {
            //Sneak: set (or update) a "keep N in stock" filter using the held amount
            int stock = held.getCount();
            if (existing >= 0) {
                stockTargets.set(existing, stock);
            } else {
                int max = PEQoLConfig.COMMON.exporterMaxFilters.get();
                if (filters.size() >= max) {
                    player.displayClientMessage(Component.translatable("message.peqol.filter_full", max).withStyle(ChatFormatting.RED), true);
                    return true;
                }
                filters.add(info);
                stockTargets.add(stock);
            }
            player.displayClientMessage(Component.translatable("message.peqol.exporter.stock", held.getHoverName(), stock).withStyle(ChatFormatting.GREEN), true);
            player.level().playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
            setChanged();
            return true;
        }
        if (existing >= 0) {
            filters.remove(existing);
            stockTargets.remove(existing);
            player.displayClientMessage(Component.translatable("message.peqol.exporter.removed", held.getHoverName()).withStyle(ChatFormatting.YELLOW), true);
            player.level().playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6F, 1F);
        } else {
            int max = PEQoLConfig.COMMON.exporterMaxFilters.get();
            if (filters.size() >= max) {
                player.displayClientMessage(Component.translatable("message.peqol.filter_full", max).withStyle(ChatFormatting.RED), true);
                return true;
            }
            filters.add(info);
            stockTargets.add(0);
            player.displayClientMessage(Component.translatable("message.peqol.exporter.added", held.getHoverName()).withStyle(ChatFormatting.GREEN), true);
            player.level().playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1F);
        }
        setChanged();
        return true;
    }

    @Override
    public void onSneakUse(ServerPlayer player) {
        if (!ownerData.canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", ownerData.describe()).withStyle(ChatFormatting.RED), true);
            return;
        }
        filters.clear();
        stockTargets.clear();
        setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.exporter.cleared").withStyle(ChatFormatting.YELLOW), true);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.exporter.status." + status.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.tier", tier, MAX_TIER).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("info.peqol.exporter.total", EmcUtil.format(itemsExported), EmcUtil.format(totalSpent)).withStyle(ChatFormatting.GOLD));
        if (filters.isEmpty()) {
            lines.add(Component.translatable("info.peqol.exporter.no_filter").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("tooltip.peqol.filter", filters.size()).withStyle(ChatFormatting.GOLD));
            for (int i = 0; i < filters.size(); i++) {
                int stock = stockTargets.get(i);
                Component mode = stock > 0 ? Component.translatable("info.peqol.exporter.keep", stock) : Component.translatable("info.peqol.exporter.unlimited");
                lines.add(Component.literal(" - ").append(filters.get(i).createStack().getHoverName()).append(" ").append(mode).withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.translatable("info.peqol.exporter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < filters.size(); i++) {
            CompoundTag entry = filters.get(i).write(new CompoundTag());
            entry.putInt("Stock", stockTargets.get(i));
            list.add(entry);
        }
        tag.put("Filters", list);
        tag.putString("TotalSpent", totalSpent.toString());
        tag.putLong("ItemsExported", itemsExported);
        tag.putInt("Tier", tier);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        filters.clear();
        stockTargets.clear();
        ListTag list = tag.getList("Filters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemInfo info = ItemInfo.read(entry);
            if (info != null) {
                filters.add(info);
                stockTargets.add(Math.max(0, entry.getInt("Stock")));
            }
        }
        try {
            String spent = tag.getString("TotalSpent");
            totalSpent = spent.isEmpty() ? BigInteger.ZERO : new BigInteger(spent);
        } catch (NumberFormatException e) {
            totalSpent = BigInteger.ZERO;
        }
        itemsExported = tag.getLong("ItemsExported");
        tier = Math.max(0, Math.min(MAX_TIER, tag.getInt("Tier")));
    }
}
