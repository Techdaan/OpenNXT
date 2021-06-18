package com.opennxt.model.entity

import com.opennxt.api.stat.StatContainer
import com.opennxt.model.commands.CommandSender
import com.opennxt.model.entity.player.InterfaceManager
import com.opennxt.model.messages.Message
import com.opennxt.model.tick.Tickable
import com.opennxt.net.ConnectedClient
import com.opennxt.net.game.GamePacket
import mu.KotlinLogging

abstract class BasePlayer(var client: ConnectedClient, val name: String): CommandSender, Tickable {
    abstract val interfaces: InterfaceManager
    abstract val stats: StatContainer

    var noTimeouts = 0
    private val logger = KotlinLogging.logger { }

    override fun message(message: Message) {
        client.write(message.createPacket())
    }

    override fun message(message: String) {
        client.write(Message.ConsoleMessage(message).createPacket())
    }

    override fun console(message: String) {
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

    fun write(message: GamePacket) {
        client.write(message)
    }
}