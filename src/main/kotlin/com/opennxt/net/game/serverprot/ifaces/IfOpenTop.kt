package com.opennxt.net.game.serverprot.ifaces

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class IfOpenTop(val id: Int): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<IfOpenTop>(fields) {
        override fun fromMap(packet: Map<String, Any>): IfOpenTop {
            return IfOpenTop(packet["id"] as Int)
        }

        override fun toMap(packet: IfOpenTop): Map<String, Any> = mapOf(
            "xtea0" to 0,
            "xtea1" to 0,
            "xtea2" to 0,
            "xtea3" to 0,
            "bool" to 0,
            "id" to packet.id
        )
    }
}