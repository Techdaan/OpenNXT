package com.opennxt.net

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

class ConnectedClient(val side: Side, val channel: Channel, var processUnidentifiedPackets: Boolean = false) {

    val logger = KotlinLogging.logger { }

    val incomingQueue = ConcurrentLinkedQueue<GamePacket>()

    fun receive(pair: OpcodeWithBuffer) {
        try {
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
                    ?: throw NullPointerException("Registration not found for packet $packet side $side")

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