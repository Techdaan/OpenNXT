package com.opennxt.net.game

import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.pipeline.GamePacketCodec

class EmptyPacketCodec<T : GamePacket>(val value: T) : GamePacketCodec<T> {
    override fun encode(packet: T, buf: GamePacketBuilder) {}

    override fun decode(buf: GamePacketReader): T = value
}