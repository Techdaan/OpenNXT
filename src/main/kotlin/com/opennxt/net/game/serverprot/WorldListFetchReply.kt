package com.opennxt.net.game.serverprot

import com.opennxt.net.buf.DataType
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketCodec

class WorldListFetchReply(val lastChunk: Boolean, val chunk: ByteArray): GamePacket {
    object Codec: GamePacketCodec<WorldListFetchReply> {
        override fun encode(packet: WorldListFetchReply, buf: GamePacketBuilder) {
            buf.put(DataType.BYTE, if (packet.lastChunk) 1 else 0)
            buf.buffer.writeBytes(packet.chunk)
        }

        override fun decode(buf: GamePacketReader): WorldListFetchReply {
            val lastChunk = buf.getUnsigned(DataType.BYTE).toInt() == 1
            val chunk = ByteArray(buf.buffer.readableBytes())
            buf.buffer.readBytes(chunk)
            return WorldListFetchReply(lastChunk, chunk)
        }
    }
}