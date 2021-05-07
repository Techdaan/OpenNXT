package com.opennxt.model.entity

import com.opennxt.model.commands.CommandSender
import com.opennxt.model.messages.Message
import com.opennxt.model.tick.Tickable
import com.opennxt.net.ConnectedClient
import mu.KotlinLogging

abstract class BasePlayer(var client: ConnectedClient): CommandSender, Tickable {
    private val logger = KotlinLogging.logger { }

    override fun message(message: Message) {
        client.write(message.createPacket())
    }

    override fun message(message: String) {
        client.write(Message.ConsoleMessage(message).createPacket())
    }

    override fun error(message: String) {
        client.write(Message.ConsoleError(message).createPacket())
    }

    override fun hasPermissions(node: String): Boolean {
        return true
    }

    override fun tick() {

    }
}