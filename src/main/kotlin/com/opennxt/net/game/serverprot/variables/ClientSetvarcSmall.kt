package com.opennxt.net.game.serverprot.variables

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class ClientSetvarcSmall(val id: Int, val value: Int): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<ClientSetvarcSmall>(fields) {
        override fun fromMap(packet: Map<String, Any>): ClientSetvarcSmall {
            return ClientSetvarcSmall(packet["id"] as Int, packet["value"] as Int)
        }

        override fun toMap(packet: ClientSetvarcSmall): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["id"] = packet.id
            map["value"] = packet.value
            return map
        }
    }
}