package com.opennxt.net.js5

import com.opennxt.ext.readBuild
import com.opennxt.ext.readString
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.js5.packet.Js5Packet
import com.opennxt.net.js5.packet.Js5PacketCodec
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mu.KotlinLogging

class Js5Decoder(val session: Js5Session) : ByteToMessageDecoder() {
    private val logger = KotlinLogging.logger { }

    var handshakeDecoded = false

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        if (!handshakeDecoded) {
            buf.markReaderIndex()
            val size = buf.readUnsignedByte().toInt()

            if (size <= 10) {
                logger.warn { "Invalid js5 handshake sent from ${ctx.channel().remoteAddress()}" }
                buf.skipBytes(buf.readableBytes())
                ctx.channel().close()
                return
            }

            if (buf.readableBytes() < size) {
                buf.resetReaderIndex()
                return
            }

            val build = buf.readBuild()
            val token = buf.readString()
            val language = buf.readUnsignedByte().toInt()

            check(!buf.isReadable) { "buffer is readable after reading js5 handshake" }

            out.add(Js5Packet.Handshake(build.major, build.minor, token, language))

            handshakeDecoded = true
            return
        }

        if (buf.readableBytes() < 10) return
        when (val opcode = buf.readUnsignedByte().toInt()) {
            Js5PacketCodec.RequestFile.opcodeLow,
            Js5PacketCodec.RequestFile.opcodeHigh,
            Js5PacketCodec.RequestFile.opcodeNxtLow,
            Js5PacketCodec.RequestFile.opcodeNxtHigh1,
            Js5PacketCodec.RequestFile.opcodeNxtHigh2 -> {
                val request = Js5PacketCodec.RequestFile.decode(GamePacketReader(buf))
                request.priority = opcode != Js5PacketCodec.RequestFile.opcodeNxtLow && opcode != Js5PacketCodec.RequestFile.opcodeNxtLow
//                logger.info { "Requested file ${request.index}, ${request.archive}. Priority: ${request.priority}" }

                if (request.priority) session.highPriorityRequests.add(request)
                else session.lowPriorityRequests.add(request)
            }

            Js5PacketCodec.ConnectionInitialized.opcode -> {
                logger.info { "Connection initialized" }
                Js5PacketCodec.ConnectionInitialized.decode(GamePacketReader(buf))
                ctx.channel().attr(Js5Session.ATTR_KEY).get().initialize()
            }

            Js5PacketCodec.RequestTermination.opcode -> {
                logger.info { "Request termination" }
                Js5PacketCodec.RequestTermination.decode(GamePacketReader(buf))
                ctx.channel().attr(Js5Session.ATTR_KEY).get().close()
            }

            Js5PacketCodec.XorRequest.opcode -> {
                val packet = Js5PacketCodec.XorRequest.decode(GamePacketReader(buf))
                logger.info { "Set XOR: ${packet.xor}" }
                ctx.channel().attr(Js5Session.XOR_KEY).set(packet.xor)
            }

            Js5PacketCodec.LoggedIn.opcode -> {
                logger.info { "Logged in" }
                Js5PacketCodec.LoggedIn.decode(GamePacketReader(buf))
                ctx.channel().attr(Js5Session.LOGGED_IN).set(true)
            }

            Js5PacketCodec.LoggedOut.opcode -> {
                logger.info { "Logged out" }
                Js5PacketCodec.LoggedOut.decode(GamePacketReader(buf))
                ctx.channel().attr(Js5Session.LOGGED_IN).set(true)
            }

            else -> {
                logger.warn { "Unknown js5 opcode $opcode from ${ctx.channel().remoteAddress()}. Skipping request" }
                buf.skipBytes(9)
            }
        }
    }
}