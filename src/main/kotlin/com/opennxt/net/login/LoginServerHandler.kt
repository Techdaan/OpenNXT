package com.opennxt.net.login

import com.opennxt.login.LoginThread
import com.opennxt.net.GenericResponse
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class LoginServerHandler : SimpleChannelInboundHandler<LoginPacket>() {
    private val logger = KotlinLogging.logger { }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        logger.info { "todo handle msg = $msg" }

        if (msg is LoginPacket.LobbyLoginRequest)
            LoginThread.login(msg) {
                logger.info { "Login state: $it" }

                val future = ctx.channel()
                    .writeAndFlush(LoginPacket.LoginResponse(it.code))
                if (it.code != GenericResponse.SUCCESSFUL) {
                    logger.info { "Closing connection" }
                    future.addListener(ChannelFutureListener.CLOSE)
                    return@login
                }

                // TODO set up channels here
            }
        else throw IllegalStateException("idk how to handle $msg!")
    }
}