package com.opennxt.net.handshake

import com.opennxt.net.js5.Js5Decoder
import com.opennxt.net.js5.Js5Encoder
import com.opennxt.net.js5.Js5Handler
import com.opennxt.net.js5.Js5Session
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class HandshakeHandler : SimpleChannelInboundHandler<HandshakeRequest>() {
    private val logger = KotlinLogging.logger { }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HandshakeRequest) {
        when (msg.type) {
            HandshakeType.JS_5 -> {
                val session = Js5Session(ctx.channel())

                // replace handler before decoder to avoid decoding packet before encoder is ready (yes this was a bug)
                ctx.pipeline().addLast("js5-encoder", Js5Encoder(session))

                ctx.pipeline().replace("handshake-handler", "js5-handler", Js5Handler(session))
                ctx.pipeline().replace("handshake-decoder", "js5-decoder", Js5Decoder(session))
            }
            else -> throw IllegalStateException("Cannot handle handshake message: $msg")
        }
    }
}