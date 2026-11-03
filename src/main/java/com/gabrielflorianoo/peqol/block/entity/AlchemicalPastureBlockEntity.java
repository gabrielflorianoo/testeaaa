package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Breeds nearby adult animals, buying each species' food with the owner's personal EMC, up to a population cap.
 */
public class AlchemicalPastureBlockEntity extends BlockEntity implements IOwnedMachine {

    private static final Item[] FOODS = {Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.WHEAT_SEEDS, Items.GOLDEN_CARROT,
            Items.DANDELION, Items.SEAGRASS, Items.SWEET_BERRIES, Items.KELP, Items.SLIME_BALL, Items.CACTUS, Items.HAY_BLOCK,
            Items.COD, Items.SALMON, Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS, Items.TORCHFLOWER_SEEDS, Items.SPIDER_EYE, Items.BAMBOO};

    private final OwnerData ownerData = new OwnerData();
    private int ticks;
    private long bred;

    public AlchemicalPastureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_PASTURE.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    /**
     * @return a food this animal accepts, or empty.
     */
    public static ItemStack foodFor(Animal animal) {
        for (Item item : FOODS) {
            ItemStack stack = new ItemStack(item);
            if (animal.isFood(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalPastureBlockEntity pasture) {
        if (++pasture.ticks % Math.max(20, PEQoLConfig.COMMON.pastureInterval.get()) != 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), pasture.ownerData.getOwner());
        if (owner != null) {
            pasture.breed((ServerLevel) level, owner);
        }
    }

    private void breed(ServerLevel level, ServerPlayer owner) {
        int radius = PEQoLConfig.COMMON.pastureRadius.get();
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(radius, 3, radius), Animal::isAlive);
        if (animals.size() >= PEQoLConfig.COMMON.pastureMaxAnimals.get()) {
            return;
        }
        int budget = PEQoLConfig.COMMON.pastureMaxAnimals.get() - animals.size();
        for (Animal animal : animals) {
            if (budget <= 1) {
                break;
            }
            if (animal.getAge() != 0 || animal.isInLove() || !animal.canFallInLove()) {
                continue;
            }
            //Only feed an animal if there's a partner of the same type that can breed too
            Animal partner = null;
            for (Animal other : animals) {
                if (other != animal && other.getType() == animal.getType() && other.getAge() == 0 && !other.isInLove() && other.canFallInLove()) {
                    partner = other;
                    break;
                }
            }
            if (partner == null) {
                continue;
            }
            ItemStack food = foodFor(animal);
            if (food.isEmpty()) {
                continue;
            }
            long cost = Math.max(1, EmcUtil.value(food)) * 2;
            if (!EmcUtil.tryRemoveEmc(owner, BigInteger.valueOf(cost))) {
                return;
            }
            animal.setInLove(owner);
            partner.setInLove(owner);
            level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + 1, animal.getZ(), 3, 0.3, 0.3, 0.3, 0);
            bred++;
            budget -= 2;
            setChanged();
        }
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.pasture.stats", bred, PEQoLConfig.COMMON.pastureMaxAnimals.get()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("info.peqol.pasture.hint", PEQoLConfig.COMMON.pastureRadius.get()).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        tag.putLong("Bred", bred);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        bred = tag.getLong("Bred");
    }
}
