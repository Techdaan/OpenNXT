package com.opennxt.net.proxy

import com.opennxt.OpenNXT
import com.opennxt.model.messages.Message
import com.opennxt.model.tick.Tickable
import com.opennxt.model.worldlist.WorldList
import com.opennxt.net.ConnectedClient
import com.opennxt.net.Side
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.serverprot.WorldListFetchReply
import io.netty.buffer.ByteBufUtil
import mu.KotlinLogging

class ConnectedProxyClient(val connection: ConnectedClient) : Tickable {
    val incomingNames =
        (if (connection.side == Side.CLIENT) OpenNXT.protocol.clientProtNames else OpenNXT.protocol.serverProtNames).reversedValues()

    private val logger = KotlinLogging.logger { }

    var hexdump = false
    var ready = false

    lateinit var other: ConnectedProxyClient

    private val worldList = WorldList()

    override fun tick() {
        if (!ready || !other.ready) return

        while (true) {
            val packet = connection.incomingQueue.poll() ?: break

            // TODO Handle packet here; allow packet to be dropped
            if (packet is UnidentifiedPacket) {
                val raw = packet.packet
                if (hexdump) {
                    logger.info {
                        "[RECV] ${connection.side}: ${raw.opcode} (name=${incomingNames[raw.opcode]}, real size=${raw.buf.readableBytes()}). Dump:\n${
                            ByteBufUtil.prettyHexDump(
                                raw.buf
                            )
                        }"
                    }
                } else {
                    logger.info {
                        "[RECV] ${connection.side}: ${raw.opcode} (name=${incomingNames[raw.opcode]}, real size=${raw.buf.readableBytes()})."
                    }
                }
            } else if (packet is ClientCheat) {
                when (packet.cheat) {
                    "hexdumpon" -> {
                        hexdump = true
                        other.hexdump = true
                        connection.write(Message.ConsoleMessage("Hexdumps turned on").createPacket())
                    }
                    "hexdumpoff" -> {
                        hexdump = false
                        other.hexdump = false
                        connection.write(Message.ConsoleMessage("Hexdumps turned off").createPacket())
                    }
                    else -> connection.write(
                        Message.ConsoleError("Unknown proxy command: '${packet.cheat}'").createPacket()
                    )
                }
            } else {
                logger.info { "[RECV] ${connection.side}: $packet" }
            }

            if (packet is WorldListFetchReply) {
                worldList.handle(packet)
            }

            other.connection.write(packet)
        }
    }
}
