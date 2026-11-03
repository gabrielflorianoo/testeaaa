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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Mines a square area below itself, layer by layer, paying a little personal EMC per block.
 * Drops with EMC become the owner's personal EMC; the rest goes into an adjacent inventory (or pops out on top).
 */
public class AlchemicalExcavatorBlockEntity extends BlockEntity implements IOwnedMachine, IUpgradeable {

    private final OwnerData ownerData = new OwnerData();
    @Nullable
    private BlockPos cursor;
    private long mined;
    private BigInteger emcGained = BigInteger.ZERO;
    private boolean finished;
    private int tier;
    private int ticks;
    private String status = "idle";
    private boolean silkTouch;
    private int fortune;

    /**
     * The virtual pickaxe used for drops (diamond, plus the installed enchantment).
     */
    private ItemStack tool() {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        if (silkTouch) {
            pick.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, 1);
        } else if (fortune > 0) {
            pick.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, fortune);
        }
        return pick;
    }

    /**
     * Right click with an enchanted book holding Silk Touch or Fortune to install it (the book is consumed).
     */
    @Override
    public boolean onItemUse(ServerPlayer player, net.minecraft.world.InteractionHand hand, ItemStack held) {
        if (!(held.getItem() instanceof net.minecraft.world.item.EnchantedBookItem)) {
            return false;
        }
        if (!ownerData.canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", ownerData.describe()).withStyle(ChatFormatting.RED), true);
            return true;
        }
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(held);
        Integer silk = enchants.get(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
        Integer luck = enchants.get(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE);
        if (silk != null) {
            silkTouch = true;
            fortune = 0;
        } else if (luck != null) {
            silkTouch = false;
            fortune = Math.min(3, luck);
        } else {
            player.displayClientMessage(Component.translatable("message.peqol.excavator.bad_book").withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        setChanged();
        player.displayClientMessage(Component.translatable(silkTouch ? "message.peqol.excavator.silk" : "message.peqol.excavator.fortune", fortune)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    public AlchemicalExcavatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_EXCAVATOR.get(), pos, state);
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

    private int radius() {
        return PEQoLConfig.COMMON.excavatorRadius.get();
    }

    /**
     * Next position in the area (row by row, layer by layer going down), or null when finished.
     */
    @Nullable
    private BlockPos advance(BlockPos current) {
        int r = radius();
        int x = current.getX() + 1;
        int y = current.getY();
        int z = current.getZ();
        if (x > worldPosition.getX() + r) {
            x = worldPosition.getX() - r;
            z++;
            if (z > worldPosition.getZ() + r) {
                z = worldPosition.getZ() - r;
                y--;
            }
        }
        if (level == null || y <= level.getMinBuildHeight()) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private BlockPos start() {
        int r = radius();
        return new BlockPos(worldPosition.getX() - r, worldPosition.getY() - 1, worldPosition.getZ() - r);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalExcavatorBlockEntity excavator) {
        if (++excavator.ticks % Math.max(1, PEQoLConfig.COMMON.excavatorInterval.get()) != 0) {
            return;
        }
        excavator.status = excavator.work((ServerLevel) level);
    }

    private String work(ServerLevel level) {
        if (finished) {
            return "finished";
        }
        if (level.hasNeighborSignal(worldPosition)) {
            return "paused";
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        if (owner == null) {
            return "owner_offline";
        }
        if (cursor == null) {
            cursor = start();
        }
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        int toMine = 1 + tier;
        //Skip over air / unbreakable blocks quickly, but bound the work per tick
        int scanned = 0;
        while (toMine > 0 && scanned < 256) {
            scanned++;
            BlockPos target = cursor;
            BlockPos next = advance(target);
            if (!level.isLoaded(target)) {
                return "unloaded";
            }
            BlockState state = level.getBlockState(target);
            if (!canMine(level, target, state)) {
                if (next == null) {
                    finished = true;
                    setChanged();
                    return "finished";
                }
                cursor = next;
                continue;
            }
            long cost = PEQoLConfig.COMMON.excavatorCostPerBlock.get();
            if (cost > 0 && !EmcUtil.tryRemoveEmc(owner, BigInteger.valueOf(cost))) {
                return "no_emc";
            }
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, target, state, fake))) {
                //Protected: skip it
                cursor = next == null ? target : next;
                continue;
            }
            List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), fake, tool());
            level.destroyBlock(target, false, fake);
            handleDrops(level, owner, drops);
            mined++;
            toMine--;
            setChanged();
            if (next == null) {
                finished = true;
                return "finished";
            }
            cursor = next;
        }
        return "mining";
    }

    private static boolean canMine(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0
                && !state.is(Blocks.BEDROCK);
    }

    private void handleDrops(ServerLevel level, ServerPlayer owner, List<ItemStack> drops) {
        IItemHandler output = output(level);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            BigInteger value = EmcUtil.stackSellValue(drop);
            if (value.signum() > 0 && PEQoLConfig.COMMON.excavatorBurnsDrops.get()) {
                EmcUtil.burn(owner, drop, true);
                emcGained = emcGained.add(value);
                continue;
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
            if (direction == Direction.DOWN) {
                continue;
            }
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
    public void onSneakUse(ServerPlayer player) {
        if (!ownerData.canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", ownerData.describe()).withStyle(ChatFormatting.RED), true);
            return;
        }
        cursor = null;
        finished = false;
        setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.excavator.restart").withStyle(ChatFormatting.YELLOW), true);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.excavator.status." + status).withStyle(ChatFormatting.AQUA));
        if (cursor != null) {
            lines.add(Component.translatable("info.peqol.excavator.layer", cursor.getY()).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("info.peqol.excavator.mined", EmcUtil.format(mined), EmcUtil.format(emcGained)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.tier", tier, MAX_TIER).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (silkTouch || fortune > 0) {
            lines.add(Component.translatable(silkTouch ? "message.peqol.excavator.silk" : "message.peqol.excavator.fortune", fortune).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable("info.peqol.excavator.hint", radius() * 2 + 1).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        if (cursor != null) {
            tag.putLong("Cursor", cursor.asLong());
        }
        tag.putLong("Mined", mined);
        tag.putString("EmcGained", emcGained.toString());
        tag.putBoolean("Finished", finished);
        tag.putInt("Tier", tier);
        tag.putBoolean("SilkTouch", silkTouch);
        tag.putInt("Fortune", fortune);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        cursor = tag.contains("Cursor") ? BlockPos.of(tag.getLong("Cursor")) : null;
        mined = tag.getLong("Mined");
        try {
            String gained = tag.getString("EmcGained");
            emcGained = gained.isEmpty() ? BigInteger.ZERO : new BigInteger(gained);
        } catch (NumberFormatException e) {
            emcGained = BigInteger.ZERO;
        }
        finished = tag.getBoolean("Finished");
        tier = Math.max(0, Math.min(MAX_TIER, tag.getInt("Tier")));
        silkTouch = tag.getBoolean("SilkTouch");
        fortune = Math.max(0, Math.min(3, tag.getInt("Fortune")));
    }
}
