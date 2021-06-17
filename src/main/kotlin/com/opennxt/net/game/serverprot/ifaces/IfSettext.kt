package com.opennxt.net.game.serverprot.ifaces

import com.opennxt.model.InterfaceHash
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class IfSettext(val parent: InterfaceHash, val text: String) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<IfSettext>(fields) {
        override fun fromMap(packet: Map<String, Any>): IfSettext {
            return IfSettext(
                InterfaceHash(packet["parent"] as Int),
                packet["text"] as String
            )
        }

        override fun toMap(packet: IfSettext): Map<String, Any> = mapOf(
            "parent" to packet.parent.hash,
            "text" to packet.text
        )
    }
}