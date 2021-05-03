package com.opennxt.net.handshake

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mu.KotlinLogging

class HandshakeDecoder: ByteToMessageDecoder() {
    val logger = KotlinLogging.logger {  }

    init {
        isSingleDecode = true
    }

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val id = buf.readUnsignedByte().toInt()
        val type = HandshakeType.fromId(id)

        if (type == null) {
            logger.warn { "Client from ${ctx.channel().remoteAddress()} attempted to handshake with unknown id: $id" }
            ctx.close()
            buf.skipBytes(buf.readableBytes())
            return
        }

        logger.info { "Received handshake from ${ctx.channel().remoteAddress()} with type $type" }
        out.add(HandshakeRequest(type))
    }
}