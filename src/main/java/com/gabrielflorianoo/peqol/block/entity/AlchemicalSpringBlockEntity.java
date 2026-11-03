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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Infinite water or lava paid with the owner's personal EMC, pushed into adjacent fluid handlers or drained by pipes.
 */
public class AlchemicalSpringBlockEntity extends BlockEntity implements IOwnedMachine {

    private static final int PUSH_INTERVAL = 10;

    private final OwnerData ownerData = new OwnerData();
    private final SpringTank tank = new SpringTank();
    private LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);
    private boolean lava;
    /**
     * mB already paid for but not yet delivered.
     */
    private int credit;
    private long bucketsDelivered;
    private int ticks;

    public AlchemicalSpringBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_SPRING.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    public Fluid fluid() {
        return lava ? Fluids.LAVA : Fluids.WATER;
    }

    /**
     * EMC per bucket: (filled bucket - empty bucket) when ProjectE values it, otherwise the configured fallback.
     */
    public long costPerBucket() {
        long filled = EmcUtil.value(new ItemStack(lava ? Items.LAVA_BUCKET : Items.WATER_BUCKET));
        long empty = EmcUtil.value(new ItemStack(Items.BUCKET));
        long diff = filled - empty;
        if (filled > 0 && diff > 0) {
            return diff;
        }
        return lava ? PEQoLConfig.COMMON.springLavaCost.get() : PEQoLConfig.COMMON.springWaterCost.get();
    }

    /**
     * Makes sure at least {@code amount} mB are paid for (buys whole buckets).
     *
     * @return how many mB are available (credit), limited to {@code amount}.
     */
    private int ensureCredit(int amount, boolean execute) {
        if (credit >= amount) {
            return amount;
        }
        ServerPlayer owner = level == null ? null : EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        if (owner == null) {
            return Math.min(credit, amount);
        }
        long cost = costPerBucket();
        int bucketsNeeded = (amount - credit + FluidType.BUCKET_VOLUME - 1) / FluidType.BUCKET_VOLUME;
        long affordable = cost <= 0 ? bucketsNeeded : EmcUtil.affordable(EmcUtil.getEmc(owner), cost);
        int buckets = (int) Math.min(bucketsNeeded, affordable);
        if (buckets <= 0) {
            return Math.min(credit, amount);
        }
        if (execute) {
            if (cost > 0 && !EmcUtil.tryRemoveEmc(owner, BigInteger.valueOf(cost).multiply(BigInteger.valueOf(buckets)))) {
                return Math.min(credit, amount);
            }
            credit += buckets * FluidType.BUCKET_VOLUME;
            bucketsDelivered += buckets;
            setChanged();
            return Math.min(credit, amount);
        }
        return Math.min(credit + buckets * FluidType.BUCKET_VOLUME, amount);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalSpringBlockEntity spring) {
        if (++spring.ticks % PUSH_INTERVAL != 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (!level.isLoaded(neighbour)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(neighbour);
            if (be == null || be instanceof AlchemicalSpringBlockEntity) {
                continue;
            }
            be.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).ifPresent(target -> {
                int accepted = target.fill(new FluidStack(spring.fluid(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) {
                    return;
                }
                int available = spring.ensureCredit(accepted, true);
                if (available <= 0) {
                    return;
                }
                int filled = target.fill(new FluidStack(spring.fluid(), available), IFluidHandler.FluidAction.EXECUTE);
                spring.credit -= filled;
            });
        }
    }

    @Override
    public void onSneakUse(ServerPlayer player) {
        if (!ownerData.canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", ownerData.describe()).withStyle(ChatFormatting.RED), true);
            return;
        }
        lava = !lava;
        credit = 0;
        setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.spring.mode", Component.translatable(fluid().getFluidType().getDescriptionId()))
                .withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.spring.fluid", Component.translatable(fluid().getFluidType().getDescriptionId()), EmcUtil.format(costPerBucket()))
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.spring.delivered", EmcUtil.format(bucketsDelivered)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.spring.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tankCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        tankCap = LazyOptional.of(() -> tank);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putBoolean("Lava", lava);
        tag.putInt("Credit", credit);
        tag.putLong("Delivered", bucketsDelivered);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        lava = tag.getBoolean("Lava");
        credit = Math.max(0, tag.getInt("Credit"));
        bucketsDelivered = tag.getLong("Delivered");
    }

    private class SpringTank implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            int amount = ensureCredit(FluidType.BUCKET_VOLUME, false);
            return amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid(), amount);
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return FluidType.BUCKET_VOLUME * 16;
        }

        @Override
        public boolean isFluidValid(int tankIndex, @NotNull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != fluid()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int available = ensureCredit(maxDrain, action.execute());
            if (available <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                credit -= available;
                setChanged();
            }
            return new FluidStack(fluid(), available);
        }
    }
}
