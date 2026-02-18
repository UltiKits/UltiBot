package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.List;

/**
 * Netty outbound handler that intercepts chat packets sent to the fake player.
 * Captures the plain text content of ClientboundSystemChatPacket for test verification.
 */
class MessageCaptureHandler extends ChannelOutboundHandlerAdapter {

    private final List<String> messages;

    MessageCaptureHandler(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSystemChatPacket chatPacket) {
            // Skip action bar messages (overlay=true), only capture chat/system messages
            if (!chatPacket.overlay()) {
                String text = chatPacket.content().getString();
                messages.add(text);
            }
        }
        super.write(ctx, msg, promise);
    }
}
