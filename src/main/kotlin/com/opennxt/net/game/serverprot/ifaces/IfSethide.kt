package com.opennxt.net.game.serverprot.ifaces

import com.opennxt.model.InterfaceHash
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class IfSethide(val parent: InterfaceHash, val hidden: Boolean) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<IfSethide>(fields) {
        override fun fromMap(packet: Map<String, Any>): IfSethide {
            return IfSethide(
                InterfaceHash(packet["parent"] as Int),
                (packet["hidden"] as Int) == 1
            )
        }

        override fun toMap(packet: IfSethide): Map<String, Any> = mapOf(
            "parent" to packet.parent.hash,
            "hidden" to if (packet.hidden) { 1 } else { 0 }
        )
    }
}