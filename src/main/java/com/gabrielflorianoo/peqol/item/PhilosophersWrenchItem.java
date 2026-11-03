package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sneak + right click a ProjectE (or QoL+) machine to pick it up with its stored EMC, inventory and settings intact.
 */
public class PhilosophersWrenchItem extends Item {

    public PhilosophersWrenchItem(Properties properties) {
        super(properties);
    }

    public static boolean canWrench(BlockState state, @Nullable BlockEntity be) {
        if (be == null || state.isAir()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || state.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        return PEQoLConfig.COMMON.wrenchNamespaces.get().contains(id.getNamespace());
    }

    /**
     * Removes the block and returns it as an item that keeps its block entity data, or empty if not allowed.
     */
    public static ItemStack pickUp(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (!canWrench(state, be) || !level.mayInteract(player, pos)) {
            return ItemStack.EMPTY;
        }
        //Respect claims / protection mods
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player))) {
            return ItemStack.EMPTY;
        }
        CompoundTag data = be.saveWithoutMetadata();
        ItemStack stack = new ItemStack(state.getBlock());
        BlockItem.setBlockEntityData(stack, be.getType(), data);
        //Remove the block entity first so the block doesn't drop its inventory (it travels inside the item instead)
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return stack;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canWrench(level.getBlockState(pos), level.getBlockEntity(pos))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack picked = pickUp(level, pos, player);
        if (picked.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!player.getInventory().add(picked)) {
            player.drop(picked, false);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 0.7F);
        player.displayClientMessage(Component.translatable("message.peqol.wrench.picked", picked.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        //Let the wrench (not the machine GUI) handle sneak clicks
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.philosophers_wrench.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.philosophers_wrench.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
