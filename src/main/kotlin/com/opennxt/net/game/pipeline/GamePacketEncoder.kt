package com.opennxt.net.game.pipeline

import com.opennxt.OpenNXT
import com.opennxt.ext.writeOpcode
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import com.opennxt.util.ISAACCipher
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import it.unimi.dsi.fastutil.ints.Int2IntMap
import mu.KotlinLogging

class GamePacketEncoder : MessageToByteEncoder<OpcodeWithBuffer>() {

    private val logger = KotlinLogging.logger { }

    private val protocol = OpenNXT.protocol

    private var inited = false

    private lateinit var isaac: ISAACCipher
    private lateinit var mapping: Int2IntMap
    private lateinit var side: Side

    private fun init(channel: Channel) {
        inited = true

        isaac = channel.attr(RSChannelAttributes.OUTGOING_ISAAC).get()
        side = channel.attr(RSChannelAttributes.SIDE).get()
        mapping = if (side == Side.CLIENT) protocol.serverProtSizes.values else protocol.clientProtSizes.values
    }

    override fun encode(ctx: ChannelHandlerContext, msg: OpcodeWithBuffer, out: ByteBuf) {
        try {
            if (!inited) init(ctx.channel())

            if (!mapping.containsKey(msg.opcode)) {
                logger.error { "No opcode->size mapping for opcode ${msg.opcode} (side=$side)" }
                ctx.channel().close()
                return
            }

            val buffer = msg.buf
            val size = mapping[msg.opcode]
            if (size == -1 && buffer.writerIndex() >= 255)
                throw IllegalStateException("Var byte packet exceeds 255 bytes: ${msg.opcode} is ${buffer.writerIndex()} bytes")
            else if (size == -2 && buffer.writerIndex() >= 65535)
                throw IllegalStateException("Var short packet exceeds 65535 bytes: ${msg.opcode} is ${buffer.writerIndex()} bytes")
            else if (size >= 0 && size != buffer.writerIndex())
                throw IllegalStateException("Encoded buffer size does not match expected size (expected: ${size}, got ${buffer.writerIndex()}) opcode ${msg.opcode}")

//            logger.info { "[SEND] ${side}: ${msg.opcode} (real size=${buffer.readableBytes()}, readable=${buffer.readableBytes()}, ${buffer.readerIndex()}). Dump:\n${ByteBufUtil.prettyHexDump(buffer)}\nreadable after: ${buffer.readableBytes()}, ${buffer.readerIndex()}" }
            synchronized(isaac) {
                out.writeOpcode(isaac, msg.opcode)
                if (size == -1) out.writeByte(buffer.writerIndex())
                else if (size == -2) out.writeShort(buffer.writerIndex())
                out.writeBytes(buffer)

                buffer.release()
            }
        } catch (e: Exception) {
            logger.error(e) { "on side $side" }
            ctx.channel().close()
        }
    }
}