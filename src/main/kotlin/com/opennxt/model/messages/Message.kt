package com.opennxt.model.messages

import com.opennxt.net.game.serverprot.MessageGame

abstract class Message {
    abstract fun createPacket(): MessageGame
}