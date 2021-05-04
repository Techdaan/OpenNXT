package com.opennxt.net.login

import com.opennxt.login.LoginThread
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.game.pipeline.DynamicPacketHandler
import com.opennxt.net.game.pipeline.GamePacketEncoder
import com.opennxt.net.game.pipeline.GamePacketFraming
import com.opennxt.util.ISAACCipher
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class LoginServerHandler : SimpleChannelInboundHandler<LoginPacket>() {
    private val logger = KotlinLogging.logger { }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        if (msg is LoginPacket.LobbyLoginRequest) {
            ctx.channel().attr(RSChannelAttributes.INCOMING_ISAAC).set(ISAACCipher(msg.header.seeds))
            ctx.channel().attr(RSChannelAttributes.OUTGOING_ISAAC)
                .set(ISAACCipher(msg.header.seeds.map { it + 50 }.toIntArray()))

            LoginThread.login(msg, ctx.channel()) {
                val future = ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(it.code.id))
                if (it.code != GenericResponse.SUCCESSFUL) {
                    future.addListener(ChannelFutureListener.CLOSE)
                    return@login
                }

                if (ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get() != null) {
                    logger.info { "Login was OK for proxy connection [client->open nxt], leaving channel management to proxy..." }
                } else {
                    ctx.channel().pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                    ctx.channel().pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                    ctx.channel().pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())

                    logger.info { "Login on [SERVER] is completed. Should add this to a map somewhere to handle" }
                }
            }
        } else throw IllegalStateException("idk how to handle $msg!")
    }
}