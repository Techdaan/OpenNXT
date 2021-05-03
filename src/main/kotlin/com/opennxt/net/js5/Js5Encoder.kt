package com.opennxt.net.js5

import com.opennxt.net.js5.packet.Js5Packet
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KotlinLogging
import kotlin.math.min

class Js5Encoder(val session: Js5Session) : MessageToByteEncoder<Js5Packet>() {
    private val logger = KotlinLogging.logger { }

    override fun encode(ctx: ChannelHandlerContext, msg: Js5Packet, out: ByteBuf) {
        when (msg) {
            is Js5Packet.HandshakeResponse -> out.writeByte(msg.code)
            is Js5Packet.Prefetches -> for (value in msg.prefetches) out.writeInt(value)
            is Js5Packet.RequestFileResponse -> {
                val xor = session.channel.attr(Js5Session.XOR_KEY).get()

                val data = msg.data
                var length = ((data.getByte(1).toInt() and 0xff) shl 24) + ((data.getByte(2)
                    .toInt() and 0xff) shl 16) + ((data.getByte(3).toInt() and 0xff) shl 8) + (data.getByte(4)
                    .toInt() and 0xff) + 5
                if (data.getByte(0).toInt() != 0) length += 4

                var remaining = length
                while (data.isReadable && remaining > 0) {
                    out.writeByte(msg.index xor xor)

                    val size = if (msg.priority) msg.archive else (msg.archive or -0x80000000)
                    out.writeByte((size shr 24) xor xor)
                    out.writeByte((size shr 16) xor xor)
                    out.writeByte((size shr 8) xor xor)
                    out.writeByte((size) xor xor)

                    for (i in 0 until min(102400 - 5, remaining)) { // - 5 due to header written above
                        out.writeByte(data.readByte().toInt() xor xor)
                        remaining--
                    }
                }

                if (remaining != 0) {
                    logger.error { "remaining != 0! ${remaining}, ${data.readableBytes()} in [${msg.index}, ${msg.archive}]" }
                }
            }
            else -> logger.warn { "I don't know how to encode $msg!" }
        }
    }
}