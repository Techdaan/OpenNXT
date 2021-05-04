package com.opennxt.net.game.pipeline

import com.opennxt.net.RSChannelAttributes
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class DynamicPacketHandler : SimpleChannelInboundHandler<OpcodeWithBuffer>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: OpcodeWithBuffer) {
        val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get()
        if (passthrough != null) {
            msg.buf.retain() // retain buf so we can pass it on
            passthrough.write(msg)
        }

        ctx.channel().attr(RSChannelAttributes.CONNECTED_CLIENT).get().receive(msg)
    }
}