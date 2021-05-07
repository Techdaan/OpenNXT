package com.opennxt.net.game.serverprot

import com.opennxt.ext.readSmartShort
import com.opennxt.net.buf.DataType
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketCodec
import mu.KotlinLogging

class MessageGame(
    val type: Int,
    val message: String,
    val intParam1: Int = 0,
    val stringParam1: String? = null,
    val stringParam2: String? = null
) : GamePacket {
    object Codec : GamePacketCodec<MessageGame> {
        private val logger = KotlinLogging.logger { }

        override fun encode(packet: MessageGame, buf: GamePacketBuilder) {
            buf.putSmart(packet.type)
            buf.put(DataType.INT, packet.intParam1)

            var hash = 0
            if (packet.stringParam1 != null) hash = hash or 0x1
            if (packet.stringParam2 != null) hash = hash or 0x2

            if (packet.stringParam1 == null && packet.stringParam2 != null) {
                logger.warn { "string 1 == null, string 2 != null, these params won't show up." }
            }

            buf.put(DataType.BYTE, hash)
            if (packet.stringParam1 != null) buf.putString(packet.stringParam1)
            if (packet.stringParam1 != null && packet.stringParam2 != null) buf.putString(packet.stringParam2)

            buf.putString(packet.message)
        }

        override fun decode(buf: GamePacketReader): MessageGame {
            val type = buf.buffer.readSmartShort()
            val hash = buf.getSigned(DataType.INT).toInt()
            val mask = buf.getUnsigned(DataType.BYTE).toInt()

            val string1 = if ((mask and 1) != 0) buf.getString() else null
            val string2 = if ((mask and 1) != 0 && (mask and 2) != 0) buf.getString() else null
            val message = buf.getString()

            return MessageGame(type, message, hash, string1, string2)
        }
    }
}