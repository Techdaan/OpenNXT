package com.opennxt.net.game.pipeline

import com.opennxt.net.game.GamePacket

interface GamePacketHandler<T : Any, P : GamePacket> {
    fun handle(context: T, packet: P)
}