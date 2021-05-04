package com.opennxt.net.game.serverprot.ifaces

import com.opennxt.model.InterfaceHash
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class IfOpenSub(val id: Int, val flag: Boolean, val parent: InterfaceHash) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<IfOpenSub>(fields) {
        override fun fromMap(packet: Map<String, Any>): IfOpenSub {
            return IfOpenSub(packet["id"] as Int, packet["flag"] as Int == 1, InterfaceHash(packet["parent"] as Int))
        }

        override fun toMap(packet: IfOpenSub): Map<String, Any> = mapOf(
            "xtea0" to 0,
            "xtea1" to 0,
            "xtea2" to 0,
            "xtea3" to 0,
            "id" to packet.id,
            "flag" to if (packet.flag) 1 else 0,
            "parent" to packet.parent.hash
        )
    }
}