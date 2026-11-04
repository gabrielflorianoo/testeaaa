package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Hourglass of Undoing: Ctrl+Z for the world. While carried it remembers the blocks you break and place;
 * right click undoes the last one (sneak: the last 8). Unbreaking a block costs its EMC value, unplacing gives
 * the item back.
 */
public class UndoHourglassItem extends MagicItem {

    public static final int MAX_HISTORY = 64;
    public static final long MAX_AGE_MS = 5 * 60 * 1000;

    public record Action(boolean broken, ResourceKey<Level> dimension, BlockPos pos, BlockState state, long time) {
    }

    private static final Map<UUID, Deque<Action>> HISTORY = new ConcurrentHashMap<>();

    public UndoHourglassItem(Properties properties) {
        super(properties);
    }

    public static void record(Player player, Level level, BlockPos pos, BlockState state, boolean broken) {
        if (level.isClientSide || state.isAir() || state.hasBlockEntity()
                || !InventoryUtil.contains(player, s -> s.getItem() instanceof UndoHourglassItem)) {
            return;
        }
        Deque<Action> history = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        history.addLast(new Action(broken, level.dimension(), pos.immutable(), state, System.currentTimeMillis()));
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public static void forget(UUID player) {
        HISTORY.remove(player);
    }

    public static int historySize(UUID player) {
        Deque<Action> history = HISTORY.get(player);
        return history == null ? 0 : history.size();
    }

    /**
     * Undoes the most recent action. Returns true when something changed.
     */
    public static boolean undoOne(ServerPlayer player) {
        Deque<Action> history = HISTORY.get(player.getUUID());
        long now = System.currentTimeMillis();
        while (history != null && !history.isEmpty()) {
            Action action = history.pollLast();
            if (now - action.time() > MAX_AGE_MS || action.dimension() != player.level().dimension()) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            if (!level.isLoaded(action.pos()) || !level.mayInteract(player, action.pos())) {
                continue;
            }
            BlockState current = level.getBlockState(action.pos());
            if (action.broken()) {
                if (!current.canBeReplaced()) {
                    continue;
                }
                long cost = EmcUtil.value(new ItemStack(action.state().getBlock()));
                if (cost <= 0 && !player.getAbilities().instabuild) {
                    continue;
                }
                if (!pay(player, cost)) {
                    history.addLast(action);
                    return false;
                }
                level.setBlock(action.pos(), action.state(), Block.UPDATE_ALL);
            } else {
                if (!current.is(action.state().getBlock())) {
                    continue;
                }
                level.removeBlock(action.pos(), false);
                ItemStack back = new ItemStack(action.state().getBlock().asItem());
                if (!back.isEmpty() && !player.getAbilities().instabuild && !player.getInventory().add(back)) {
                    player.drop(back, false);
                }
            }
            BlockPos p = action.pos();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        int times = player.isShiftKeyDown() ? 8 : 1;
        int undone = 0;
        for (int i = 0; i < times && undoOne(serverPlayer); i++) {
            undone++;
        }
        if (undone == 0) {
            player.displayClientMessage(Component.translatable("message.peqol.undo.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.displayClientMessage(Component.translatable("message.peqol.undo.done", undone, historySize(player.getUUID())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 0.6F);
        player.getCooldowns().addCooldown(this, 4);
        return InteractionResultHolder.success(stack);
    }
}
