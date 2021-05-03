package com.opennxt.net.js5

import com.opennxt.Js5Thread
import com.opennxt.OpenNXT
import com.opennxt.net.js5.packet.Js5Packet
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging

class Js5Handler(val session: Js5Session): SimpleChannelInboundHandler<Js5Packet>() {
    private val logger = KotlinLogging.logger {  }

    var handledHandshake = false

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Js5Packet) {
        when(msg) {
            is Js5Packet.Handshake -> {
                if (handledHandshake)
                    throw IllegalStateException("Already handled handshake")
                handledHandshake = true

                logger.warn { "Sending OK - TODO: Check build & js5 token prior to accepting this connection" }

                ctx.channel().write(Js5Packet.HandshakeResponse(0))
                ctx.channel().write(Js5Packet.Prefetches(OpenNXT.prefetches.entries))
                ctx.channel().flush()
            }
            else -> TODO("Encode $msg")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(cause) { "Caught exception" }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        // remove session if the client connection drops and doesn't send the termination packet
        Js5Thread.removeSession(session)
    }
}