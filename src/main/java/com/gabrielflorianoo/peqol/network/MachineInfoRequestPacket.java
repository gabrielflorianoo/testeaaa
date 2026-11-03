package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.block.entity.IOwnedMachine;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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

    /**
     * Last (stored EMC, game time) seen per block entity, to compute an EMC/s rate between requests.
     */
    private static final Map<BlockEntity, long[]> SAMPLES = new WeakHashMap<>();

    private static long sampleRate(BlockEntity be, long stored) {
        if (be.getLevel() == null) {
            return 0;
        }
        long now = be.getLevel().getGameTime();
        long[] previous = SAMPLES.put(be, new long[]{stored, now});
        if (previous == null || now <= previous[1] || now - previous[1] > 100) {
            return 0;
        }
        return (stored - previous[0]) * 20 / (now - previous[1]);
    }

    private static void describeProjectE(BlockEntity be, List<Component> lines) {
        if (be instanceof moze_intel.projecte.gameObjs.block_entities.CondenserBlockEntity condenser) {
            moze_intel.projecte.api.ItemInfo lock = condenser.getLockInfo();
            if (lock == null) {
                lines.add(Component.translatable("info.peqol.condenser.no_target").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("info.peqol.condenser.target", lock.createStack().getHoverName()).withStyle(ChatFormatting.AQUA));
                if (condenser.requiredEmc > 0) {
                    long percent = Math.min(100, condenser.getStoredEmc() * 100 / condenser.requiredEmc);
                    lines.add(Component.translatable("info.peqol.condenser.progress", percent).withStyle(ChatFormatting.GOLD));
                }
            }
        } else if (be instanceof moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity collector && be.getLevel() != null) {
            lines.add(Component.translatable("info.peqol.collector.sun", collector.getSunLevel() - 1).withStyle(ChatFormatting.GOLD));
            long toGoal = collector.getEmcToNextGoal();
            if (toGoal > 0) {
                lines.add(Component.translatable("info.peqol.collector.goal", EmcUtil.format(toGoal)).withStyle(ChatFormatting.GRAY));
            }
        }
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
            long rate = sampleRate(be, storage.getStoredEmc());
            if (rate != 0) {
                lines.add(Component.translatable("info.peqol.rate", (rate > 0 ? "+" : "") + EmcUtil.format(rate))
                        .withStyle(rate > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        try {
            describeProjectE(be, lines);
        } catch (Throwable ignored) {
            //Internal ProjectE classes changed: only the generic info is shown
        }
        return lines;
    }
}
