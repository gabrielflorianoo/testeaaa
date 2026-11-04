package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Architect's Bottle: sneak + right click a block to mark the first corner, right click another block to bottle
 * everything between them. Right click with a full bottle to pour the build out again, turned to face the same way
 * relative to you. Containers and other block entities are left in place.
 */
public class ArchitectBottleItem extends MagicItem {

    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, PEQoL.rl("architect_blacklist"));
    public static final String TAG_CORNER = "CornerA";
    public static final String TAG_CORNER_DIM = "CornerDim";
    public static final String TAG_PALETTE = "Palette";
    public static final String TAG_BLOCKS = "Blocks";
    public static final String TAG_FACING = "Facing";

    private static final int REMOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public ArchitectBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.architectMaxSize.get(), EmcUtil.format(PEQoLConfig.COMMON.architectCostPerBlock.get())};
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_BLOCKS);
    }

    public static int storedCount(ItemStack stack) {
        return isFilled(stack) ? stack.getTag().getIntArray(TAG_BLOCKS).length / 4 : 0;
    }

    public static boolean capturable(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && !(state.getBlock() instanceof LiquidBlock) && state.getDestroySpeed(level, pos) >= 0
                && !state.is(BLACKLIST);
    }

    /**
     * Blocks between the corners that can be bottled, or null when the area is too big.
     */
    @Nullable
    public static List<BlockPos> selection(Level level, BlockPos a, BlockPos b) {
        int max = PEQoLConfig.COMMON.architectMaxSize.get();
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos maxPos = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        if (maxPos.getX() - min.getX() >= max || maxPos.getY() - min.getY() >= max || maxPos.getZ() - min.getZ() >= max) {
            return null;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, maxPos)) {
            if (capturable(level, pos, level.getBlockState(pos))) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    /**
     * Moves the given blocks into the bottle (relative to their minimum corner) and removes them from the world.
     */
    public static int capture(ItemStack bottle, Level level, List<BlockPos> positions, Direction facing, @Nullable Player player) {
        List<BlockPos> taken = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (player == null || (level.mayInteract(player, pos) && !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)))) {
                taken.add(pos);
            }
        }
        if (taken.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (BlockPos pos : taken) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        Map<BlockState, Integer> palette = new HashMap<>();
        ListTag paletteTag = new ListTag();
        int[] data = new int[taken.size() * 4];
        for (int i = 0; i < taken.size(); i++) {
            BlockPos pos = taken.get(i);
            BlockState state = level.getBlockState(pos);
            Integer index = palette.get(state);
            if (index == null) {
                index = palette.size();
                palette.put(state, index);
                paletteTag.add(NbtUtils.writeBlockState(state));
            }
            data[i * 4] = pos.getX() - minX;
            data[i * 4 + 1] = pos.getY() - minY;
            data[i * 4 + 2] = pos.getZ() - minZ;
            data[i * 4 + 3] = index;
        }
        //Top to bottom, without drops or neighbour updates, so attached blocks don't pop off and duplicate
        taken.sort((p1, p2) -> Integer.compare(p2.getY(), p1.getY()));
        for (BlockPos pos : taken) {
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.getFluidState().createLegacyBlock(), REMOVE_FLAGS);
        }
        for (BlockPos pos : taken) {
            level.blockUpdated(pos, level.getBlockState(pos).getBlock());
        }
        CompoundTag tag = bottle.getOrCreateTag();
        tag.put(TAG_PALETTE, paletteTag);
        tag.putIntArray(TAG_BLOCKS, data);
        tag.putInt(TAG_FACING, facing.get2DDataValue());
        tag.remove(TAG_CORNER);
        tag.remove(TAG_CORNER_DIM);
        return taken.size();
    }

    public static Rotation rotation(int fromFacing, int toFacing) {
        return switch (Math.floorMod(toFacing - fromFacing, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos rotate(int x, int y, int z, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-z, y, x);
            case CLOCKWISE_180 -> new BlockPos(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    /**
     * Pours the bottled blocks out, bottom centred on {@code target}. Returns the amount placed, or -1 when
     * something is in the way.
     */
    public static int place(ItemStack bottle, Level level, BlockPos target, Direction facing, @Nullable Player player) {
        if (!isFilled(bottle)) {
            return 0;
        }
        CompoundTag tag = bottle.getTag();
        ListTag paletteTag = tag.getList(TAG_PALETTE, Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), paletteTag.getCompound(i)));
        }
        int[] data = tag.getIntArray(TAG_BLOCKS);
        Rotation rotation = rotation(tag.getInt(TAG_FACING), facing.get2DDataValue());
        int count = data.length / 4;
        BlockPos[] relative = new BlockPos[count];
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            relative[i] = rotate(data[i * 4], data[i * 4 + 1], data[i * 4 + 2], rotation);
            minX = Math.min(minX, relative[i].getX());
            maxX = Math.max(maxX, relative[i].getX());
            minZ = Math.min(minZ, relative[i].getZ());
            maxZ = Math.max(maxZ, relative[i].getZ());
        }
        int offsetX = target.getX() - Math.floorDiv(minX + maxX, 2);
        int offsetZ = target.getZ() - Math.floorDiv(minZ + maxZ, 2);
        BlockPos[] world = new BlockPos[count];
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(relative[i].getX() + offsetX, relative[i].getY() + target.getY(), relative[i].getZ() + offsetZ);
            if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced() || (player != null && !level.mayInteract(player, pos))) {
                return -1;
            }
            world[i] = pos;
        }
        for (int i = 0; i < count; i++) {
            int index = data[i * 4 + 3];
            if (index >= 0 && index < palette.size()) {
                level.setBlock(world[i], palette.get(index).rotate(rotation), PLACE_FLAGS);
            }
        }
        for (BlockPos pos : world) {
            level.blockUpdated(pos, level.getBlockState(pos).getBlock());
        }
        tag.remove(TAG_PALETTE);
        tag.remove(TAG_BLOCKS);
        tag.remove(TAG_FACING);
        return count;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack bottle = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction facing = player.getDirection();
        if (isFilled(bottle)) {
            BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
            int placed = place(bottle, level, target, facing, player);
            if (placed < 0) {
                player.displayClientMessage(Component.translatable("message.peqol.architect.blocked").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, 30, 1.5, 1, 1.5, 0.02);
            level.playSound(null, target, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1F, 0.6F);
            level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.2F);
            player.displayClientMessage(Component.translatable("message.peqol.architect.placed", placed).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag tag = bottle.getOrCreateTag();
        if (player.isShiftKeyDown() || !tag.contains(TAG_CORNER) || !tag.getString(TAG_CORNER_DIM).equals(level.dimension().location().toString())) {
            tag.putLong(TAG_CORNER, clicked.asLong());
            tag.putString(TAG_CORNER_DIM, level.dimension().location().toString());
            player.displayClientMessage(Component.translatable("message.peqol.architect.corner", clicked.getX(), clicked.getY(), clicked.getZ()).withStyle(ChatFormatting.AQUA), true);
            serverLevel.sendParticles(ParticleTypes.END_ROD, clicked.getX() + 0.5, clicked.getY() + 1.1, clicked.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.01);
            return InteractionResult.CONSUME;
        }
        BlockPos corner = BlockPos.of(tag.getLong(TAG_CORNER));
        List<BlockPos> selection = selection(level, corner, clicked);
        if (selection == null) {
            player.displayClientMessage(Component.translatable("message.peqol.architect.too_big", PEQoLConfig.COMMON.architectMaxSize.get()).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (selection.isEmpty()) {
            return InteractionResult.FAIL;
        }
        long cost = selection.size() * PEQoLConfig.COMMON.architectCostPerBlock.get();
        if (!player.getAbilities().instabuild && EmcUtil.getEmc(player).compareTo(BigInteger.valueOf(cost)) < 0) {
            pay(serverPlayer, cost);
            return InteractionResult.FAIL;
        }
        int taken = capture(bottle, level, selection, facing, player);
        if (taken > 0) {
            pay(serverPlayer, taken * PEQoLConfig.COMMON.architectCostPerBlock.get());
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, (corner.getX() + clicked.getX()) / 2.0 + 0.5, (corner.getY() + clicked.getY()) / 2.0 + 0.5,
                    (corner.getZ() + clicked.getZ()) / 2.0 + 0.5, 60, Math.abs(corner.getX() - clicked.getX()) / 2.0, Math.abs(corner.getY() - clicked.getY()) / 2.0,
                    Math.abs(corner.getZ() - clicked.getZ()) / 2.0, 0.05);
            level.playSound(null, clicked, SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1F, 0.8F);
            player.displayClientMessage(Component.translatable("message.peqol.architect.captured", taken).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isFilled(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (isFilled(stack)) {
            tooltip.add(Component.translatable("tooltip.peqol.architect.holds", storedCount(stack)).withStyle(ChatFormatting.AQUA));
        } else if (stack.hasTag() && stack.getTag().contains(TAG_CORNER)) {
            BlockPos c = BlockPos.of(stack.getTag().getLong(TAG_CORNER));
            tooltip.add(Component.translatable("message.peqol.architect.corner", c.getX(), c.getY(), c.getZ()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.peqol.architect.empty").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
