package com.opennxt.net.game.serverprot.variables

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class ClientSetvarcstrSmall(val id: Int, val value: String): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<ClientSetvarcstrSmall>(fields) {
        override fun fromMap(packet: Map<String, Any>): ClientSetvarcstrSmall {
            return ClientSetvarcstrSmall(packet["id"] as Int, packet["value"] as String)
        }

        override fun toMap(packet: ClientSetvarcstrSmall): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["id"] = packet.id
            map["value"] = packet.value
            return map
        }
    }

    override fun toString(): String = "ClientSetvarcstrSmall(id=$id, value=\"$value\")"
}