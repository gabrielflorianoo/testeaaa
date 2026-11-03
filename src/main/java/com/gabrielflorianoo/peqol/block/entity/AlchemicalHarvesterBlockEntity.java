package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Harvests mature crops around it (same layer and one below), replanting them. Produce with EMC becomes the owner's
 * personal EMC (or goes into an adjacent inventory if burning is disabled / the owner is offline).
 */
public class AlchemicalHarvesterBlockEntity extends BlockEntity implements IOwnedMachine, IUpgradeable {

    public enum HarvestType {
        NONE, REPLANT, BREAK, CUT_ABOVE
    }

    private final OwnerData ownerData = new OwnerData();
    private int tier;
    private int ticks;
    private long harvested;
    private BigInteger emcGained = BigInteger.ZERO;

    public AlchemicalHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_HARVESTER.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public void setTier(int tier) {
        this.tier = Math.max(0, Math.min(MAX_TIER, tier));
        setChanged();
    }

    public int radius() {
        return PEQoLConfig.COMMON.harvesterRadius.get() + 2 * tier;
    }

    /**
     * How a block should be harvested, if at all.
     */
    public static HarvestType classify(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? HarvestType.REPLANT : HarvestType.NONE;
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3 ? HarvestType.REPLANT : HarvestType.NONE;
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            return HarvestType.BREAK;
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
            //Only cut segments that have the same plant below them (keep the base)
            return level.getBlockState(pos.below()).is(block) ? HarvestType.CUT_ABOVE : HarvestType.NONE;
        }
        return HarvestType.NONE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalHarvesterBlockEntity harvester) {
        if (++harvester.ticks % Math.max(1, PEQoLConfig.COMMON.harvesterInterval.get()) != 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        harvester.harvest((ServerLevel) level);
    }

    private void harvest(ServerLevel level) {
        if (!ownerData.hasOwner()) {
            return;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        int r = radius();
        boolean changed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    HarvestType type = classify(level, cursor, state);
                    if (type == HarvestType.NONE) {
                        continue;
                    }
                    BlockPos target = cursor.immutable();
                    if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, target, state, fake))) {
                        continue;
                    }
                    List<ItemStack> drops = Block.getDrops(state, level, target, null, fake, ItemStack.EMPTY);
                    if (type == HarvestType.REPLANT) {
                        //Keep one seed back to replant
                        ItemStack seed = state.getBlock().getCloneItemStack(level, target, state);
                        for (ItemStack drop : drops) {
                            if (!seed.isEmpty() && ItemStack.isSameItem(drop, seed) && drop.getCount() > 0) {
                                drop.shrink(1);
                                break;
                            }
                        }
                        BlockState replanted = state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(0)
                                : state.setValue(NetherWartBlock.AGE, 0);
                        level.setBlock(target, replanted, Block.UPDATE_ALL);
                    } else {
                        level.destroyBlock(target, false, fake);
                    }
                    handleDrops(level, owner, drops);
                    harvested++;
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    private void handleDrops(ServerLevel level, @Nullable ServerPlayer owner, List<ItemStack> drops) {
        IItemHandler output = output(level);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            if (owner != null && PEQoLConfig.COMMON.harvesterBurnsProduce.get()) {
                BigInteger gained = EmcUtil.burn(owner, drop, true);
                if (gained.signum() > 0) {
                    emcGained = emcGained.add(gained);
                    continue;
                }
            }
            ItemStack leftover = output == null ? drop : ItemHandlerHelper.insertItemStacked(output, drop, false);
            if (!leftover.isEmpty()) {
                Block.popResource(level, worldPosition.above(), leftover);
            }
        }
    }

    @Nullable
    private IItemHandler output(Level level) {
        for (Direction direction : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
            if (be != null && !(be instanceof IOwnedMachine)) {
                IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.vacuum.radius", radius()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.harvester.total", EmcUtil.format(harvested), EmcUtil.format(emcGained)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.tier", tier, MAX_TIER).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("info.peqol.harvester.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putInt("Tier", tier);
        tag.putLong("Harvested", harvested);
        tag.putString("EmcGained", emcGained.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        tier = Math.max(0, Math.min(MAX_TIER, tag.getInt("Tier")));
        harvested = tag.getLong("Harvested");
        try {
            String gained = tag.getString("EmcGained");
            emcGained = gained.isEmpty() ? BigInteger.ZERO : new BigInteger(gained);
        } catch (NumberFormatException e) {
            emcGained = BigInteger.ZERO;
        }
    }
}
