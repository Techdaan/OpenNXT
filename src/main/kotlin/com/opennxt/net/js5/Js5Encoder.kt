package com.opennxt.net.js5

import com.opennxt.net.js5.packet.Js5Packet
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KotlinLogging

class Js5Encoder(val session: Js5Session) : MessageToByteEncoder<Js5Packet>() {
    private val logger = KotlinLogging.logger {  }

    override fun encode(ctx: ChannelHandlerContext, msg: Js5Packet, out: ByteBuf) {
        when (msg) {
            is Js5Packet.HandshakeResponse -> out.writeByte(msg.code)
            is Js5Packet.Prefetches -> for(value in msg.prefetches) out.writeInt(value)
            else -> logger.warn { "I don't know how to encode $msg!" }
        }
    }
}