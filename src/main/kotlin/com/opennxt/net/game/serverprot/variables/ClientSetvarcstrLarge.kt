package com.opennxt.net.game.serverprot.variables

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class ClientSetvarcstrLarge(val id: Int, val value: String): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<ClientSetvarcstrLarge>(fields) {
        override fun fromMap(packet: Map<String, Any>): ClientSetvarcstrLarge {
            return ClientSetvarcstrLarge(packet["id"] as Int, packet["value"] as String)
        }

        override fun toMap(packet: ClientSetvarcstrLarge): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["id"] = packet.id
            map["value"] = packet.value
            return map
        }
    }

    override fun toString(): String = "ClientSetvarcstrLarge(id=$id, value=\"$value\")"
}