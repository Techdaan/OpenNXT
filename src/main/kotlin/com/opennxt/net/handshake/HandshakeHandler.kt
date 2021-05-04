package com.opennxt.net.handshake

import com.opennxt.OpenNXT
import com.opennxt.net.ConnectedClient
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import com.opennxt.net.js5.Js5Decoder
import com.opennxt.net.js5.Js5Encoder
import com.opennxt.net.js5.Js5Handler
import com.opennxt.net.js5.Js5Session
import com.opennxt.net.login.LoginServerDecoder
import com.opennxt.net.login.LoginEncoder
import com.opennxt.net.login.LoginPacket
import com.opennxt.net.login.LoginServerHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

class HandshakeHandler : SimpleChannelInboundHandler<HandshakeRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HandshakeRequest) {
        ctx.channel().attr(RSChannelAttributes.SIDE).set(Side.CLIENT)
        ctx.channel().attr(RSChannelAttributes.CONNECTED_CLIENT).set(ConnectedClient(Side.CLIENT, ctx.channel()))

        when (msg.type) {
            HandshakeType.JS_5 -> {
                val session = Js5Session(ctx.channel())

                // replace handler before decoder to avoid decoding packet before encoder is ready (yes this was a bug)
                ctx.pipeline().addLast("js5-encoder", Js5Encoder(session))

                ctx.pipeline().replace("handshake-handler", "js5-handler", Js5Handler(session))
                ctx.pipeline().replace("handshake-decoder", "js5-decoder", Js5Decoder(session))
            }
            HandshakeType.LOGIN -> {
                val uniqueId = ThreadLocalRandom.current().nextLong()

                ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).set(uniqueId)

                ctx.pipeline().addLast("login-encoder", LoginEncoder())

                ctx.pipeline().replace("handshake-handler", "login-handler", LoginServerHandler())
                ctx.pipeline().replace("handshake-decoder", "login-decoder", LoginServerDecoder(OpenNXT.rsaConfig.login))

                ctx.channel().write(LoginPacket.LoginResponse(GenericResponse.SUCCESSFUL_CONNECTION))
                ctx.channel().write(LoginPacket.SendUniqueId(uniqueId))
                ctx.channel().flush()
            }
            else -> throw IllegalStateException("Cannot handle handshake message: $msg")
        }
    }
}