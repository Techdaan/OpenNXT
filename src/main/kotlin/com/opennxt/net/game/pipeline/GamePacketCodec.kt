package com.opennxt.net.game.pipeline

import com.opennxt.net.PacketCodec
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

abstract class GamePacketCodec<T : GamePacket> : PacketCodec<T> {
    override fun encode(packet: T, buf: GamePacketBuilder) {
        val fields = toFields(packet)


    }

    override fun decode(buf: GamePacketReader): T {
        val fields = Object2ObjectOpenHashMap<String, Any>()



        return fromFields(fields)
    }

    protected abstract fun fromFields(packet: Map<String, Any>): T

    protected abstract fun toFields(packet: T): Map<String, Any>
}