package com.opennxt.net.game.serverprot

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class ChatFilterSettingsPrivatechat(val value: Int): GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>): DynamicGamePacketCodec<ChatFilterSettingsPrivatechat>(fields) {
        override fun fromMap(packet: Map<String, Any>): ChatFilterSettingsPrivatechat {
            return ChatFilterSettingsPrivatechat(packet["value"] as Int)
        }

        override fun toMap(packet: ChatFilterSettingsPrivatechat): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["value"] = packet.value
            return map
        }
    }
}