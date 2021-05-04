package com.opennxt.net.game.serverprot

import com.opennxt.net.game.GamePacket

object NoTimeout: GamePacket {
    override fun toString(): String = "NoTimeout"
}