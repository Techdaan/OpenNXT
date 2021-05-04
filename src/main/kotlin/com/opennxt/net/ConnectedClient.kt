package com.opennxt.net

import com.opennxt.OpenNXT
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.PacketRegistry
import com.opennxt.net.game.pipeline.GamePacketCodec
import com.opennxt.net.game.pipeline.OpcodeWithBuffer
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import mu.KotlinLogging

class ConnectedClient(val side: Side, val channel: Channel) {
    val incomingNames = if (side == Side.CLIENT) OpenNXT.protocol.clientProtNames else OpenNXT.protocol.serverProtNames

    val logger = KotlinLogging.logger { }

    fun receive(pair: OpcodeWithBuffer) {
        try {
            val registration = PacketRegistry.getRegistration(side, pair.opcode)
            if (registration == null) {
                logger.info { "Received packet w/o codec [opcode=${pair.opcode}, name=${incomingNames.reversedValues()[pair.opcode]}] on side $side" }
                return
            }

            val decoded = registration.codec.decode(GamePacketReader(pair.buf))

            logger.info { "Received packet [opcode=${pair.opcode}, name=${incomingNames.reversedValues()[pair.opcode]}] on side $side: $decoded" }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pair.buf.release()
        }
    }

    fun handle() {

    }

    fun write(pair: OpcodeWithBuffer) {
        channel.write(pair)
    }

    fun write(packet: GamePacket) {
        try {
            val registration = PacketRegistry.getRegistration(side, packet::class)
                ?: throw NullPointerException("Registration not found for packet $packet")

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