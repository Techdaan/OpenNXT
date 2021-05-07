package com.opennxt.net.game.pipeline

import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging
import java.util.*

class DynamicPacketHandler : SimpleChannelInboundHandler<OpcodeWithBuffer>() {
    private val logger = KotlinLogging.logger { }

    // Some packets are sent by the other side *before* the other side is ready, this is a hacky workaround to fix this.
    // Ideally we'd queue packets regardless and send next server tick, but we'll need world ticking for that.
    //
    // TODO Resolve this hacky workaround.
    // TODO IF we do keep this method, we should 100% check queue if we have to write.
    private val queue = LinkedList<OpcodeWithBuffer>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: OpcodeWithBuffer) {
//        var written = false
//        val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get()
        try {
//            val side = ctx.channel().attr(RSChannelAttributes.SIDE).get()
//            logger.info { "channel read0 $side: $msg" }
//
//            if (passthrough != null) {
//                val copy = OpcodeWithBuffer(msg.opcode, msg.buf.copy())
//
//                if (passthrough.pipeline().get("game-encoder") == null) {
//                    // queue until we can send packets
//                    queue += copy
//                } else {
//                    while (queue.isNotEmpty()) {
//                        val next = queue.pollFirst() ?: break
//                        passthrough.write(next).addListener {
//                            if (!it.isSuccess)
//                                logger.error(it.cause()) { "Failed to passthrough packet with opcode ${next.opcode}" }
//                        }
//                    }
//
//                    passthrough.write(copy).addListener {
//                        if (!it.isSuccess)
//                            logger.error(it.cause()) { "Failed to passthrough packet with opcode ${copy.opcode}" }
//                    }
//
//                    written = true
//                }
//            }

            ctx.channel().attr(RSChannelAttributes.CONNECTED_CLIENT).get().receive(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
//            if (written && passthrough != null)
//                passthrough.flush()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error(cause) { "Exception caught in packet handler" }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info { "Channel on side ${ctx.channel().attr(RSChannelAttributes.SIDE).get()} went inactive" }
        val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get()
        if (passthrough != null && passthrough.isOpen) {
            passthrough.close()
        }
    }
}