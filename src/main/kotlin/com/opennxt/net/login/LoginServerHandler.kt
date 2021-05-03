package com.opennxt.net.login

import com.opennxt.net.GenericResponse
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class LoginServerHandler: SimpleChannelInboundHandler<LoginPacket>() {
    private val logger = KotlinLogging.logger {  }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        logger.info { "todo handle msg = $msg" }

        ctx.channel()
            .writeAndFlush(LoginPacket.Response(GenericResponse.LOGINSERVER_REJECTED))
            .addListener(ChannelFutureListener.CLOSE)
    }
}