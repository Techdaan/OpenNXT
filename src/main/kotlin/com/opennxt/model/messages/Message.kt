package com.opennxt.model.messages

import com.opennxt.net.game.serverprot.MessageGame

abstract class Message {
    abstract fun createPacket(): MessageGame

    open class SimpleMessage(val type: Int, val message: String): Message() {
        override fun createPacket(): MessageGame = MessageGame(type, message)
    }

    class ConsoleError(message: String): SimpleMessage(96, message)
    class ConsoleAutocomplete(message: String): SimpleMessage(98, message)
    class ConsoleMessage(message: String): SimpleMessage(99, message)
}