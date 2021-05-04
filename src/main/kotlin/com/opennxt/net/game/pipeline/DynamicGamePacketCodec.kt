package com.opennxt.net.game.pipeline

import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.protocol.PacketFieldDeclaration
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

abstract class DynamicGamePacketCodec<T : GamePacket>(val fields: Array<PacketFieldDeclaration>) : GamePacketCodec<T> {

    override fun encode(packet: T, buf: GamePacketBuilder) {
        val map = toMap(packet)

        fields.forEach { field ->
            val id = field.key
            val value = map[id] ?: throw NullPointerException("Packet field missing: ${this::class.simpleName}: $id")

            field.dataCodec.write(buf, value)
        }
    }

    override fun decode(buf: GamePacketReader): T {
        val map = Object2ObjectOpenHashMap<String, Any>()

        fields.forEach { field ->
            map[field.key] = field.dataCodec.read(buf)
        }

        return fromMap(map)
    }

    protected abstract fun fromMap(packet: Map<String, Any>): T

    protected abstract fun toMap(packet: T): Map<String, Any>

}