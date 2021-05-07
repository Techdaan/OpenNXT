package com.opennxt.net.game.serverprot

import com.opennxt.net.buf.DataType
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketCodec

// a: no clue
// b: autocomplete "root"
class ConsoleFeedback(val a: String, val b: String, val totalMatches: Int, val matches: Collection<String>) :
    GamePacket {
    object Codec : GamePacketCodec<ConsoleFeedback> {
        override fun encode(packet: ConsoleFeedback, buf: GamePacketBuilder) {
            buf.putString(packet.a)
            buf.putString(packet.b)
            buf.put(DataType.INT, packet.totalMatches)
            buf.put(DataType.SHORT, packet.matches.size)
            packet.matches.forEach(buf::putString)
        }

        override fun decode(buf: GamePacketReader): ConsoleFeedback = ConsoleFeedback(
            buf.getString(),
            buf.getString(),
            buf.getSigned(DataType.INT).toInt(),
            List(buf.getUnsigned(DataType.SHORT).toInt()) { buf.getString() }
        )
    }
}