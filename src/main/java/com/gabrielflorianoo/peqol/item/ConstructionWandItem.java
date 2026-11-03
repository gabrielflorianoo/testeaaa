package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click a block face: copies of that block are placed over every connected block of the same kind on that face.
 * Uses matching blocks from the inventory first, then personal EMC (for learned blocks).
 * Sneak + right click in the air cycles the size.
 */
public class ConstructionWandItem extends Item {

    public static final String TAG_SIZE = "WandSize";
    public static final int[] SIZES = {1, 9, 25, 49, 81};

    public ConstructionWandItem(Properties properties) {
        super(properties);
    }

    public static int getSize(ItemStack stack) {
        int index = stack.hasTag() && stack.getTag().contains(TAG_SIZE) ? stack.getTag().getInt(TAG_SIZE) : 2;
        index = Math.max(0, Math.min(SIZES.length - 1, index));
        return Math.min(SIZES[index], PEQoLConfig.COMMON.wandMaxBlocks.get());
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            int index = stack.hasTag() && stack.getTag().contains(TAG_SIZE) ? stack.getTag().getInt(TAG_SIZE) : 2;
            index = (index + 1) % SIZES.length;
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
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos origin = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(origin);
        if (!WandPlacement.canCopy(state)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!PEQoLConfig.COMMON.wandEnabled.get() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        List<BlockPos> targets = WandPlacement.findTargets(level, origin, face, getSize(context.getItemInHand()));
        if (targets.isEmpty()) {
            return InteractionResult.FAIL;
        }
        int placed = place(serverPlayer, level, state, face, targets, context.getItemInHand());
        if (placed > 0) {
            SoundType sound = state.getSoundType(level, origin, player);
            level.playSound(null, origin, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);
        }
        return placed > 0 ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static int place(ServerPlayer player, Level level, BlockState state, Direction face, List<BlockPos> targets, ItemStack wand) {
        Item blockItem = state.getBlock().asItem();
        ItemStack template = new ItemStack(blockItem);
        boolean creative = player.getAbilities().instabuild;
        Inventory inventory = player.getInventory();

        int inInventory = 0;
        if (!creative) {
            for (ItemStack stack : inventory.items) {
                if (stack.is(blockItem) && !stack.hasTag()) {
                    inInventory += stack.getCount();
                }
            }
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        long unit = EmcUtil.value(template);
        boolean emcAllowed = !creative && PEQoLConfig.COMMON.wandUsesEmc.get() && provider != null && unit > 0 && provider.hasKnowledge(template);
        long fromEmc = emcAllowed ? EmcUtil.affordable(provider.getEmc(), unit) : 0;
        long budget = creative ? Long.MAX_VALUE : inInventory + fromEmc;
        if (budget <= 0) {
            player.displayClientMessage(Component.translatable("message.peqol.wand.no_blocks", template.getHoverName()).withStyle(ChatFormatting.RED), true);
            return 0;
        }

        int placed = 0;
        for (BlockPos target : targets) {
            if (placed >= budget) {
                break;
            }
            if (!level.mayInteract(player, target) || !player.mayUseItemAt(target, face, wand)) {
                continue;
            }
            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, target);
            if (!level.setBlock(target, state, Block.UPDATE_ALL)) {
                continue;
            }
            if (ForgeEventFactory.onBlockPlace(player, snapshot, face)) {
                //Cancelled by a protection mod / claim
                snapshot.restore(true, false);
                continue;
            }
            placed++;
        }
        if (placed == 0 || creative) {
            return placed;
        }

        //Pay: inventory first, then EMC
        int toTake = Math.min(placed, inInventory);
        int remaining = toTake;
        for (int i = 0; i < inventory.items.size() && remaining > 0; i++) {
            ItemStack stack = inventory.items.get(i);
            if (stack.is(blockItem) && !stack.hasTag()) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        int paidWithEmc = placed - toTake;
        if (paidWithEmc > 0 && provider != null) {
            provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(paidWithEmc))));
            provider.syncEmc(player);
        }
        return placed;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.peqol.wand.size", getSize(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.peqol.construction_wand.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.construction_wand.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
