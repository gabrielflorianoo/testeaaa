package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.event.CommonEvents;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.registry.ModStats;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.function.Supplier;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: the player middle clicked a block they don't carry; create it from personal EMC.
 */
public record PickBlockPacket(BlockPos pos) {

    public static void encode(PickBlockPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static PickBlockPacket decode(FriendlyByteBuf buf) {
        return new PickBlockPacket(buf.readBlockPos());
    }

    public static void handle(PickBlockPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild) {
            pick(player, packet.pos);
        }
        context.get().setPacketHandled(true);
    }

    private static void pick(ServerPlayer player, BlockPos pos) {
        if (!PEQoLConfig.COMMON.pickBlockEnabled.get()) {
            return;
        }
        if (PEQoLConfig.COMMON.pickBlockRequiresRestocker.get() && !CommonEvents.hasActive(player, RestockerTalismanItem.class)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 12 * 12) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        ItemStack picked = state.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), level, pos, player);
        if (picked.isEmpty() || picked.hasTag() || EmcUtil.isBlacklisted(picked)) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (inventory.findSlotMatchingItem(picked) != -1) {
            //Vanilla pick block already handles it
            return;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null || !provider.hasKnowledge(picked)) {
            player.displayClientMessage(Component.translatable("message.peqol.pick.unknown", picked.getHoverName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        long unit = EmcUtil.value(picked);
        if (unit <= 0) {
            return;
        }
        int amount = (int) Math.min(Math.min(PEQoLConfig.COMMON.pickBlockAmount.get(), picked.getMaxStackSize()), EmcUtil.affordable(provider.getEmc(), unit));
        if (amount <= 0) {
            player.displayClientMessage(Component.translatable("command.peqol.not_enough_emc").withStyle(ChatFormatting.RED), true);
            return;
        }
        int hotbarSlot = inventory.getSuitableHotbarSlot();
        if (!inventory.getItem(hotbarSlot).isEmpty() && inventory.getFreeSlot() == -1) {
            //Picking would destroy the item in the hotbar slot
            player.displayClientMessage(Component.translatable("message.peqol.pick.full").withStyle(ChatFormatting.RED), true);
            return;
        }
        picked.setCount(amount);
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(amount))));
        provider.syncEmc(player);
        inventory.setPickedItem(picked);
        player.awardStat(ModStats.ITEMS_CREATED, amount);
        player.connection.send(new ClientboundSetCarriedItemPacket(inventory.selected));
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4F, 1.8F);
    }
}
