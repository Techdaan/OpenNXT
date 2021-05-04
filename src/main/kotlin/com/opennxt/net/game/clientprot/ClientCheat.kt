package com.opennxt.net.game.clientprot

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class ClientCheat(val forced: Boolean, val tabbed: Boolean, val cheat: String) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<ClientCheat>(fields) {
        override fun fromMap(packet: Map<String, Any>): ClientCheat {
            return ClientCheat(packet["forced"] as Int == 1, packet["tabbed"] as Int == 1, packet["cheat"] as String)
        }

        override fun toMap(packet: ClientCheat): Map<String, Any> = mapOf(
            "forced" to if (packet.forced) 1 else 0,
            "tabbed" to if (packet.tabbed) 1 else 0,
            "cheat" to packet.cheat
        )
    }


    override fun toString(): String = "ClientCheat(bool1=$forced, tabbed=$tabbed, cheat=\"$cheat\")"
}