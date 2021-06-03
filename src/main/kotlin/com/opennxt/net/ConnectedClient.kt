package com.opennxt.net

import com.opennxt.model.proxy.PacketDumper
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.PacketRegistry
import com.opennxt.net.game.pipeline.GamePacketCodec
import com.opennxt.net.game.pipeline.OpcodeWithBuffer
import com.opennxt.net.proxy.UnidentifiedPacket
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles incoming packets on the lowest possible level. This is usually called directly from the Netty pipeline, and
 *   the [incomingQueue] is polled from the main thread. This way packets are received and decoded async, and handled
 *   sync.
 *
 * This also handles sending packets to the other side of the channel.
 *
 * This class can be used for both the server (Where clients connect to) and the client (Which connects to a server).
 *   It is, for example, used in the proxy as well.
 *
 * [side] represents the side of the remote. This means the server uses side "Client".
 */
class ConnectedClient(
    val side: Side,
    val channel: Channel,
    var processUnidentifiedPackets: Boolean = false,
    var dumper: PacketDumper? = null
) {

    val logger = KotlinLogging.logger { }

    val incomingQueue = ConcurrentLinkedQueue<GamePacket>()

    fun receive(pair: OpcodeWithBuffer) {
        try {
            dumper?.dump(pair.opcode, pair.buf)

            val registration = PacketRegistry.getRegistration(side, pair.opcode)
            if (registration == null) {
                if (processUnidentifiedPackets)
                    incomingQueue.add(UnidentifiedPacket(OpcodeWithBuffer(pair.opcode, pair.buf.copy())))
                return
            }

            val decoded = registration.codec.decode(GamePacketReader(pair.buf))

            incomingQueue.add(decoded)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pair.buf.release()
        }
    }

    fun write(pair: OpcodeWithBuffer) {
        channel.write(pair)
    }

    fun write(packet: GamePacket) {
        if (packet is UnidentifiedPacket) {
            write(packet.packet)
            return
        }

        try {
            val registration =
                PacketRegistry.getRegistration(if (side == Side.CLIENT) Side.SERVER else Side.CLIENT, packet::class)

            if (registration == null) {
                logger.warn("Registration not found for packet $packet side $side")
                return
            }

            val buffer = Unpooled.buffer()
            @Suppress("UNCHECKED_CAST")
            (registration.codec as GamePacketCodec<GamePacket>).encode(packet, GamePacketBuilder(buffer))

            channel.write(OpcodeWithBuffer(registration.opcode, buffer))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun flush() {
        channel.flush()
    }
}