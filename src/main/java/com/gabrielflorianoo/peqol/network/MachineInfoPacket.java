package com.gabrielflorianoo.peqol.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -> client: lines describing the machine at {@code pos}.
 */
public record MachineInfoPacket(BlockPos pos, List<Component> lines) {

    private static final int MAX_LINES = 12;

    public static void encode(MachineInfoPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        int count = Math.min(MAX_LINES, packet.lines.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeComponent(packet.lines.get(i));
        }
    }

    public static MachineInfoPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = Math.min(MAX_LINES, buf.readVarInt());
        List<Component> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readComponent());
        }
        return new MachineInfoPacket(pos, lines);
    }

    public static void handle(MachineInfoPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabrielflorianoo.peqol.client.MachineInfoOverlay.INSTANCE.receive(packet.pos, packet.lines));
        context.get().setPacketHandled(true);
    }
}
