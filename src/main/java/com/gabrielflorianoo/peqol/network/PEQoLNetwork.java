package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.PEQoL;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PEQoLNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(PEQoL.rl("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private PEQoLNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(KeyActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(KeyActionPacket::encode)
                .decoder(KeyActionPacket::decode)
                .consumerMainThread(KeyActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(PickBlockPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickBlockPacket::encode)
                .decoder(PickBlockPacket::decode)
                .consumerMainThread(PickBlockPacket::handle)
                .add();
        CHANNEL.messageBuilder(MachineInfoRequestPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MachineInfoRequestPacket::encode)
                .decoder(MachineInfoRequestPacket::decode)
                .consumerMainThread(MachineInfoRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(MachineInfoPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MachineInfoPacket::encode)
                .decoder(MachineInfoPacket::decode)
                .consumerMainThread(MachineInfoPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
