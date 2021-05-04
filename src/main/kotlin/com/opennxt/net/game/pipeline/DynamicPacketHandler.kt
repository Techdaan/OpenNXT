package com.opennxt.net.game.pipeline

import com.opennxt.OpenNXT
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import com.opennxt.util.ISAACCipher
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import mu.KotlinLogging

class DynamicPacketHandler : SimpleChannelInboundHandler<OpcodeWithBuffer>() {

    private val logger = KotlinLogging.logger { }

    private val protocol = OpenNXT.protocol

    private var inited = false

    private lateinit var isaac: ISAACCipher
    private lateinit var mapping: Int2IntMap
    private lateinit var names: Int2ObjectMap<String>
    private lateinit var side: Side

    private fun init(channel: Channel) {
        inited = true

        isaac = channel.attr(RSChannelAttributes.OUTGOING_ISAAC).get()
        side = channel.attr(RSChannelAttributes.SIDE).get()
        mapping = if (side == Side.CLIENT) protocol.serverProtSizes.values else protocol.clientProtSizes.values
        names =
            if (side == Side.CLIENT) protocol.serverProtNames.reversedValues() else protocol.clientProtNames.reversedValues()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: OpcodeWithBuffer) {
        val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get()
        if (passthrough != null) {
            msg.buf.retain() // retain buf so we can pass it on
            passthrough.write(msg)
        }

        if (!names.containsKey(msg.opcode)) {
            logger.warn { "Received unknown opcode ${msg.opcode} on side $side, dropping" }
            return
        }


    }
}