package com.opennxt.net.game.serverprot.ifaces

import com.opennxt.model.InterfaceHash
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class IfSetevents(val parent: InterfaceHash, val fromSlot: Int, val toSlot: Int, val mask: Int) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<IfSetevents>(fields) {
        override fun fromMap(packet: Map<String, Any>): IfSetevents {
            return IfSetevents(
                InterfaceHash(packet["parent"] as Int),
                packet["fromSlot"] as Int,
                packet["toSlot"] as Int,
                packet["mask"] as Int
            )
        }

        override fun toMap(packet: IfSetevents): Map<String, Any> = mapOf(
            "parent" to packet.parent.hash,
            "fromSlot" to packet.fromSlot,
            "toSlot" to packet.toSlot,
            "mask" to packet.mask
        )
    }
}