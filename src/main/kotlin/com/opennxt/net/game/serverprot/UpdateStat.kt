package com.opennxt.net.game.serverprot

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class UpdateStat(val stat: Int, val level: Int, val experience: Int) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<UpdateStat>(fields) {
        override fun fromMap(packet: Map<String, Any>): UpdateStat {
            return UpdateStat(packet["stat"] as Int, packet["level"] as Int, packet["experience"] as Int)
        }

        override fun toMap(packet: UpdateStat): Map<String, Any> {
            val map = Object2ObjectOpenHashMap<String, Any>()
            map["stat"] = packet.stat
            map["level"] = packet.level
            map["experience"] = packet.experience
            return map
        }
    }
}