package com.opennxt.net.proxy

import com.opennxt.OpenNXT
import com.opennxt.model.tick.Tickable
import com.opennxt.net.ConnectedClient
import com.opennxt.net.Side
import io.netty.buffer.ByteBufUtil
import mu.KotlinLogging

class ConnectedProxyClient(val connection: ConnectedClient) : Tickable {
    val incomingNames =
        (if (connection.side == Side.CLIENT) OpenNXT.protocol.clientProtNames else OpenNXT.protocol.serverProtNames).reversedValues()

    private val logger = KotlinLogging.logger { }

    var ready = false

    lateinit var other: ConnectedProxyClient

    override fun tick() {
        if (!ready || !other.ready) return

        while (true) {
            val packet = connection.incomingQueue.poll() ?: break

            // TODO Handle packet here; allow packet to be dropped
            if (packet is UnidentifiedPacket) {
                val raw = packet.packet
                logger.info {
                    "[RECV] ${connection.side}: ${raw.opcode} (name=${incomingNames[raw.opcode]}, real size=${raw.buf.readableBytes()}). Dump:\n${
                        ByteBufUtil.prettyHexDump(
                            raw.buf
                        )
                    }"
                }
            } else {
                logger.info { "[RECV] ${connection.side}: $packet" }
            }

            other.connection.write(packet)
        }
    }
}
