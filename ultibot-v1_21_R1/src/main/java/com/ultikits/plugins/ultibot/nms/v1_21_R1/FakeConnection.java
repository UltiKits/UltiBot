package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakeConnection extends Connection {

    private final List<String> capturedMessages = new CopyOnWriteArrayList<>();

    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        // Add outbound handler to capture chat packets before they hit the channel buffer
        channel.pipeline().addFirst("message_capture", new MessageCaptureHandler(capturedMessages));
        this.channel = channel;
        // Set a dummy address so handleDisconnection() doesn't NPE
        this.address = new InetSocketAddress("127.0.0.1", 0);
    }

    /**
     * Returns a snapshot of all captured messages (plain text, no color codes).
     */
    public List<String> getCapturedMessages() {
        return new ArrayList<>(capturedMessages);
    }

    /**
     * Clears all captured messages.
     */
    public void clearCapturedMessages() {
        capturedMessages.clear();
    }
}
