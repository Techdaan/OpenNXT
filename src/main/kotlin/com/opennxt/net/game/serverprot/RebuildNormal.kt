package com.opennxt.net.game.serverprot

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicGamePacketCodec
import com.opennxt.net.game.protocol.PacketFieldDeclaration

data class RebuildNormal(
    val unused1: Int,
    val chunkX: Int,
    val unused2: Int,
    val chunkY: Int,
    val npcBits: Int,
    val mapSize: Int,
    val areaType: Int,
    val hash1: Int,
    val hash2: Int
) : GamePacket {
    class Codec(fields: Array<PacketFieldDeclaration>) : DynamicGamePacketCodec<RebuildNormal>(fields) {
        override fun fromMap(packet: Map<String, Any>): RebuildNormal {
            return RebuildNormal(
                packet["unused1"] as Int,
                packet["chunkX"] as Int,
                packet["unused2"] as Int,
                packet["chunkY"] as Int,
                packet["npcBits"] as Int,
                packet["mapSize"] as Int,
                packet["areaType"] as Int,
                packet["hash1"] as Int,
                packet["hash2"] as Int
            )
        }

        override fun toMap(packet: RebuildNormal): Map<String, Any> = mapOf(
            "unused1" to packet.unused1,
            "unused2" to packet.unused2,
            "mapSize" to packet.mapSize,
            "chunkX" to packet.chunkX,
            "chunkY" to packet.chunkY,
            "npcBits" to packet.npcBits,

            "areaType" to packet.areaType,
            "hash1" to packet.hash1,
            "hash2" to packet.hash2
        )
    }
}