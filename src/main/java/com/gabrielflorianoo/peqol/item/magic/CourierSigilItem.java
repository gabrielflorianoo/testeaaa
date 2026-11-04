package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Courier's Sigil: sneak + right click a container to bind it. Afterwards right click to send the stack in your
 * other hand to it, or sneak + right click in the air to send your whole backpack (not the hotbar), from any
 * distance or dimension.
 */
public class CourierSigilItem extends MagicItem {

    public static final String TAG_TARGET = "CourierTarget";

    public CourierSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.courierCostPerStack.get())};
    }

    @Nullable
    public static GlobalPos getTarget(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_TARGET)) {
            return null;
        }
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, stack.getTag().get(TAG_TARGET)).result().orElse(null);
    }

    public static void setTarget(ItemStack stack, GlobalPos pos) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(tag -> stack.getOrCreateTag().put(TAG_TARGET, tag));
    }

    @Nullable
    private static IItemHandler handler(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (handler(level, context.getClickedPos()) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            setTarget(context.getItemInHand(), GlobalPos.of(level.dimension(), context.getClickedPos()));
            player.displayClientMessage(Component.translatable("message.peqol.courier.bound", level.getBlockState(context.getClickedPos()).getBlock().getName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            level.playSound(null, context.getClickedPos(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack sigil = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(sigil);
        }
        GlobalPos target = getTarget(sigil);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.peqol.courier.unbound").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(sigil);
        }
        ServerLevel targetLevel = serverPlayer.server.getLevel(target.dimension());
        if (targetLevel == null) {
            return InteractionResultHolder.fail(sigil);
        }
        //Loads the chunk if needed
        targetLevel.getChunkAt(target.pos());
        IItemHandler handler = handler(targetLevel, target.pos());
        if (handler == null) {
            player.displayClientMessage(Component.translatable("message.peqol.courier.missing").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(sigil);
        }
        long cost = PEQoLConfig.COMMON.courierCostPerStack.get();
        int sent = 0;
        int itemsSent = 0;
        if (player.isShiftKeyDown()) {
            for (int slot = 9; slot < 36; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                int moved = send(serverPlayer, handler, stack, cost);
                if (moved < 0) {
                    break;
                }
                if (moved > 0) {
                    sent++;
                    itemsSent += moved;
                }
            }
        } else {
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(other);
            if (!stack.isEmpty()) {
                int moved = send(serverPlayer, handler, stack, cost);
                if (moved > 0) {
                    sent++;
                    itemsSent += moved;
                }
            }
        }
        if (sent == 0) {
            player.displayClientMessage(Component.translatable("message.peqol.courier.nothing").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(sigil);
        }
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.courier.sent", itemsSent).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.4);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.4F);
        return InteractionResultHolder.success(sigil);
    }

    /**
     * Moves as much of the stack as fits. Returns the amount moved, or -1 when the player can't pay.
     */
    private static int send(ServerPlayer player, IItemHandler handler, ItemStack stack, long cost) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), true);
        int moved = stack.getCount() - remainder.getCount();
        if (moved <= 0) {
            return 0;
        }
        if (!pay(player, cost)) {
            return -1;
        }
        remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        moved = stack.getCount() - remainder.getCount();
        stack.setCount(remainder.getCount());
        return moved;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        GlobalPos target = getTarget(stack);
        if (target == null) {
            tooltip.add(Component.translatable("message.peqol.courier.unbound").withStyle(ChatFormatting.YELLOW));
        } else {
            BlockPos p = target.pos();
            tooltip.add(Component.translatable("tooltip.peqol.courier.target", p.getX(), p.getY(), p.getZ(), target.dimension().location().toString())
                    .withStyle(ChatFormatting.AQUA));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

}
