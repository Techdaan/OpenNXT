package com.opennxt.net.handshake

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class HandshakeHandler: SimpleChannelInboundHandler<HandshakeRequest>() {
    private val logger = KotlinLogging.logger {  }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HandshakeRequest) {
        logger.info { "received handshake $msg" }
    }
}