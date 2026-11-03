package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.block.entity.IOwnedMachine;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Client -> server: "what is this machine I'm looking at?". Answered with {@link MachineInfoPacket}.
 */
public record MachineInfoRequestPacket(BlockPos pos) {

    public static void encode(MachineInfoRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static MachineInfoRequestPacket decode(FriendlyByteBuf buf) {
        return new MachineInfoRequestPacket(buf.readBlockPos());
    }

    public static void handle(MachineInfoRequestPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.level().isLoaded(packet.pos) && player.distanceToSqr(Vec3.atCenterOf(packet.pos)) < 16 * 16) {
            List<Component> lines = describe(player.level().getBlockEntity(packet.pos));
            PEQoLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MachineInfoPacket(packet.pos, lines));
        }
        context.get().setPacketHandled(true);
    }

    public static List<Component> describe(BlockEntity be) {
        List<Component> lines = new ArrayList<>();
        if (be == null) {
            return lines;
        }
        if (be instanceof IOwnedMachine machine) {
            machine.addInfo(lines);
            //Drop the trailing hint lines in the compact HUD view
            lines.removeIf(line -> line.getStyle().getColor() != null && line.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)));
            return lines;
        }
        IEmcStorage storage = be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY).resolve().orElse(null);
        if (storage != null) {
            lines.add(Component.translatable("message.peqol.lens.storage", EmcUtil.format(storage.getStoredEmc()), EmcUtil.format(storage.getMaximumEmc(), true))
                    .withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }
}
