package com.opennxt.net.game.serverprot

import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketCodec

class MessageGame(

): GamePacket {
    object Codec: GamePacketCodec<GamePacket> {
        override fun encode(packet: GamePacket, buf: GamePacketBuilder) {
            TODO("Not yet implemented")
        }

        override fun decode(buf: GamePacketReader): GamePacket {
            TODO("Not yet implemented")
        }
    }
}