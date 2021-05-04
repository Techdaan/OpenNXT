package com.opennxt.net

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

    val logger = KotlinLogging.logger {  }

    fun receive(pair: OpcodeWithBuffer) {
        try {
            val registration = PacketRegistry.getRegistration(side, pair.opcode) ?: return

            val decoded = registration.codec.decode(GamePacketReader(pair.buf))

            logger.info { "Received / decoded packet $decoded" }
            println("decoded packet $decoded")
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