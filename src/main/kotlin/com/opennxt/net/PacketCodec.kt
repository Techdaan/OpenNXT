package com.opennxt.net

import com.opennxt.net.buf.BitBuf

interface PacketCodec<T: Packet> {
    fun encode(packet: T, buf: BitBuf)

    fun decode(buf: BitBuf): T
}