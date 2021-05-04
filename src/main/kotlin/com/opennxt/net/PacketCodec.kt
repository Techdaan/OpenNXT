package com.opennxt.net

import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader

interface PacketCodec<T: Packet> {
    fun encode(packet: T, buf: GamePacketBuilder)

    fun decode(buf: GamePacketReader): T
}