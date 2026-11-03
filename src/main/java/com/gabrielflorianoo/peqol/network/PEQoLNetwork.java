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
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
