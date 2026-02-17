package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

public class FakeConnection extends Connection {
    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        // Create a fake channel that absorbs all packets
        this.channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    }
}
