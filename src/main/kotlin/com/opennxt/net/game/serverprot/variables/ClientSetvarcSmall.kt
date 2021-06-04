package com.opennxt.net.game.serverprot.variables

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class ClientSetvarcSmall(val id: Int, val value: Int): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<ClientSetvarcSmall>(fields) {
        override fun fromMap(packet: Map<String, Any>): ClientSetvarcSmall {
            var value = packet["value"] as Int
            if (value == 255) value = -1
            return ClientSetvarcSmall(packet["id"] as Int, value)
        }

        override fun toMap(packet: ClientSetvarcSmall): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["id"] = packet.id
            map["value"] = packet.value
            return map
        }
    }
}