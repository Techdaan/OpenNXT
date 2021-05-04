package com.opennxt.net.game.pipeline

import com.opennxt.OpenNXT
import com.opennxt.ext.isBigOpcode
import com.opennxt.ext.readOpcode
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import com.opennxt.util.ISAACCipher
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import it.unimi.dsi.fastutil.ints.Int2IntMap
import mu.KotlinLogging

class GamePacketFraming : ByteToMessageDecoder() {

    private val logger = KotlinLogging.logger { }

    private val protocol = OpenNXT.protocol

    private var inited = false
    private lateinit var isaac: ISAACCipher
    private lateinit var mapping: Int2IntMap
    private lateinit var side: Side

    private var state = State.READ_OPCODE
    private var opcode = -1
    private var size = -1

    private fun init(channel: Channel) {
        inited = true

        isaac = channel.attr(RSChannelAttributes.INCOMING_ISAAC).get()
        side = channel.attr(RSChannelAttributes.SIDE).get()
        mapping = if (side == Side.CLIENT) protocol.clientProtSizes.values else protocol.serverProtSizes.values
    }

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        try {
            if (!inited) init(ctx.channel())

            while (buf.isReadable) {
                if (state == State.READ_OPCODE) {
                    if (!buf.isReadable) return

                    if (buf.readableBytes() < 2 && buf.isBigOpcode(isaac)) {
                        logger.info { "is big opcode:  true, readable is 1, need to wait!" }
                        return
                    }

                    opcode = buf.readOpcode(isaac)
                    if (!mapping.containsKey(opcode)) {
                        logger.error { "No opcode->size mapping for opcode $opcode (side=$side)" }
                        buf.skipBytes(buf.readableBytes())
                        ctx.channel().close()
                        return
                    }

                    size = mapping[opcode]
                    state = if (size >= 0) State.READ_BODY else State.READ_SIZE
                }

                if (state == State.READ_SIZE && size < 0) {
                    if (buf.readableBytes() < -size) return

                    size = if (size == -1) buf.readUnsignedByte().toInt() else buf.readUnsignedShort()

                    state = State.READ_BODY
                }

                if (state == State.READ_BODY) {
                    if (buf.readableBytes() < size) return

                    val payload = buf.readBytes(size)
                    // TODO if proxy and side is client -> dump ?
//                    logger.info { "Received side=$side opcode=$opcode size=$size name=${if (side == Side.SERVER) OpenNXT.protocol.serverProtNames.reversedValues()[opcode] ?: "null" else "null"}" }

                    out.add(OpcodeWithBuffer(opcode, payload))

                    state = State.READ_OPCODE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private enum class State {
        READ_OPCODE,
        READ_SIZE,
        READ_BODY
    }
}