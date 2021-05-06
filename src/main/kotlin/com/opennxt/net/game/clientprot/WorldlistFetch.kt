package com.opennxt.net.game.clientprot

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

class WorldlistFetch(val checksum: Int) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<WorldlistFetch>(fields) {
        override fun fromMap(packet: Map<String, Any>): WorldlistFetch {
            return WorldlistFetch(packet["checksum"] as Int)
        }

        override fun toMap(packet: WorldlistFetch): Map<String, Any> = mapOf(
            "checksum" to packet.checksum
        )
    }
}