package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transmutation Scepter: sneak + right click a block to imprint it, then right click blocks to transmute them into
 * the imprinted block. The old blocks are burned into EMC, the new ones are created from EMC (must be learned).
 * Sneak + right click in the air changes the area (1 / 9 / 25 connected blocks on the face).
 */
public class TransmutationScepterItem extends MagicItem {

    public static final String TAG_BLOCK = "ImprintedBlock";
    public static final String TAG_SIZE = "ScepterSize";
    private static final int[] SIZES = {1, 9, 25};

    public TransmutationScepterItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static Block getImprint(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_BLOCK)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString(TAG_BLOCK));
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }

    public static int getSize(ItemStack stack) {
        int index = stack.hasTag() ? stack.getTag().getInt(TAG_SIZE) : 0;
        return SIZES[Math.floorMod(index, SIZES.length)];
    }

    private static boolean transmutable(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0
                && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR;
    }

    /**
     * Connected blocks of the same kind on the clicked face's plane, closest first (including the clicked one).
     */
    public static List<BlockPos> area(Level level, BlockPos origin, Direction face, int max) {
        List<BlockPos> result = new ArrayList<>();
        Block block = level.getBlockState(origin).getBlock();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty() && result.size() < max) {
            BlockPos pos = queue.poll();
            if (!level.getBlockState(pos).is(block)) {
                continue;
            }
            result.add(pos);
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() != face.getAxis()) {
                    BlockPos next = pos.relative(direction);
                    if (Math.abs(next.getX() - origin.getX()) <= 2 && Math.abs(next.getY() - origin.getY()) <= 2
                            && Math.abs(next.getZ() - origin.getZ()) <= 2 && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide) {
            int index = (stack.getOrCreateTag().getInt(TAG_SIZE) + 1) % SIZES.length;
            stack.getOrCreateTag().putInt(TAG_SIZE, index);
            player.displayClientMessage(Component.translatable("message.peqol.wand.size", getSize(stack)).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (player == null || !transmutable(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack scepter = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            scepter.getOrCreateTag().putString(TAG_BLOCK, String.valueOf(id));
            player.displayClientMessage(Component.translatable("message.peqol.scepter.imprint", state.getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            return InteractionResult.CONSUME;
        }
        Block imprint = getImprint(scepter);
        if (imprint == null) {
            player.displayClientMessage(Component.translatable("message.peqol.scepter.no_imprint").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        if (state.is(imprint)) {
            return InteractionResult.FAIL;
        }
        ItemStack newItem = new ItemStack(imprint);
        long newValue = EmcUtil.value(newItem);
        if (!player.getAbilities().instabuild && (newValue <= 0 || !EmcUtil.isKnown(player, newItem) || EmcUtil.isBlacklisted(newItem))) {
            player.displayClientMessage(Component.translatable("message.peqol.pick.unknown", newItem.getHoverName()).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockState newState = imprint.defaultBlockState();
        int changed = 0;
        for (BlockPos target : area(level, pos, context.getClickedFace(), getSize(scepter))) {
            BlockState old = level.getBlockState(target);
            if (!transmutable(level, target, old) || !level.mayInteract(player, target)
                    || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, target, old, player))) {
                continue;
            }
            long refund = EmcUtil.sellValue(new ItemStack(old.getBlock()));
            if (!player.getAbilities().instabuild) {
                long net = newValue - refund;
                if (net > 0 && !EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(net))) {
                    player.displayClientMessage(Component.translatable("command.peqol.not_enough_emc").withStyle(ChatFormatting.RED), true);
                    break;
                }
                if (net < 0) {
                    EmcUtil.addEmc(serverPlayer, BigInteger.valueOf(-net));
                }
            }
            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, target);
            level.setBlock(target, newState, Block.UPDATE_ALL);
            if (ForgeEventFactory.onBlockPlace(player, snapshot, context.getClickedFace())) {
                snapshot.restore(true, false);
                continue;
            }
            serverLevel.sendParticles(ParticleTypes.WITCH, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
            changed++;
        }
        if (changed > 0) {
            level.playSound(null, pos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return changed > 0 ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        Block imprint = getImprint(stack);
        tooltip.add(imprint == null ? Component.translatable("message.peqol.scepter.no_imprint").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.peqol.scepter.imprint", imprint.getName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.peqol.wand.size", getSize(stack)).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
