package com.gabrielflorianoo.peqol.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record KeyActionPacket(KeyAction action) {

    public static void encode(KeyActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
    }

    public static KeyActionPacket decode(FriendlyByteBuf buf) {
        return new KeyActionPacket(buf.readEnum(KeyAction.class));
    }

    public static void handle(KeyActionPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.isAlive() && !player.isSpectator()) {
            ServerActions.handle(player, packet.action);
        }
        context.get().setPacketHandled(true);
    }
}
