package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

/**
 * An Importer that also sucks item entities in a radius around it (mob farms, tree farms, quarries...).
 * Optional whitelist: right click with an item to (un)filter it; with no filter every item with EMC is taken.
 */
public class AlchemicalVacuumBlockEntity extends AlchemicalImporterBlockEntity implements IUpgradeable {

    private final List<ResourceLocation> filter = new ArrayList<>();
    private int vacuumTicks;
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

    public int getRadius() {
        return PEQoLConfig.COMMON.vacuumRadius.get() + 2 * tier;
    }

    public AlchemicalVacuumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_VACUUM.get(), pos, state);
    }

    public List<ResourceLocation> getFilter() {
        return filter;
    }

    public boolean accepts(ItemStack stack) {
        if (filter.isEmpty()) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && filter.contains(id);
    }

    public static void vacuumTick(Level level, BlockPos pos, BlockState state, AlchemicalVacuumBlockEntity vacuum) {
        AlchemicalImporterBlockEntity.serverTick(level, pos, state, vacuum);
        if (++vacuum.vacuumTicks % Math.max(1, PEQoLConfig.COMMON.vacuumInterval.get() / (1 + vacuum.tier)) != 0 || !vacuum.getOwnerData().hasOwner() || level.hasNeighborSignal(pos)) {
            return;
        }
        int radius = vacuum.getRadius();
        AABB area = new AABB(pos).inflate(radius);
        boolean any = false;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();
            if (!vacuum.accepts(stack)) {
                continue;
            }
            if (vacuum.importStack(stack.copy(), false)) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.2, entity.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
                }
                entity.discard();
                any = true;
            }
        }
        if (any) {
            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.15F, 1.6F);
        }
    }

    @Override
    public boolean onItemUse(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (!getOwnerData().canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", getOwnerData().describe()).withStyle(ChatFormatting.RED), true);
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (id == null) {
            return true;
        }
        if (filter.remove(id)) {
            player.displayClientMessage(Component.translatable("message.peqol.siphon.removed", held.getHoverName()).withStyle(ChatFormatting.YELLOW), true);
        } else {
            if (!EmcUtil.hasValue(held)) {
                player.displayClientMessage(Component.translatable("message.peqol.no_emc", held.getHoverName()).withStyle(ChatFormatting.RED), true);
                return true;
            }
            int max = PEQoLConfig.COMMON.siphonMaxFilters.get();
            if (filter.size() >= max) {
                player.displayClientMessage(Component.translatable("message.peqol.filter_full", max).withStyle(ChatFormatting.RED), true);
                return true;
            }
            filter.add(id);
            player.displayClientMessage(Component.translatable("message.peqol.siphon.added", held.getHoverName()).withStyle(ChatFormatting.GREEN), true);
        }
        setChanged();
        return true;
    }

    @Override
    public void onSneakUse(ServerPlayer player) {
        if (!getOwnerData().canConfigure(player)) {
            player.displayClientMessage(Component.translatable("message.peqol.not_owner", getOwnerData().describe()).withStyle(ChatFormatting.RED), true);
            return;
        }
        filter.clear();
        setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.exporter.cleared").withStyle(ChatFormatting.YELLOW), true);
    }

    @Override
    public void addInfo(List<Component> lines) {
        super.addInfo(lines);
        lines.add(Component.translatable("info.peqol.vacuum.radius", getRadius()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("info.peqol.tier", tier, MAX_TIER).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (filter.isEmpty()) {
            lines.add(Component.translatable("info.peqol.vacuum.everything").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("tooltip.peqol.filter", filter.size()).withStyle(ChatFormatting.GOLD));
            for (ResourceLocation id : filter) {
                Item item = ForgeRegistries.ITEMS.getValue(id);
                lines.add(Component.literal(" - ").append(item == null ? Component.literal(id.toString()) : item.getDescription()).withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.translatable("info.peqol.vacuum.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (ResourceLocation id : filter) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("VacuumFilter", list);
        tag.putInt("Tier", tier);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        tier = Math.max(0, Math.min(MAX_TIER, tag.getInt("Tier")));
        filter.clear();
        ListTag list = tag.getList("VacuumFilter", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                filter.add(id);
            }
        }
    }
}
