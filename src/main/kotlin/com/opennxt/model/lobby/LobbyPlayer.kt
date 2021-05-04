package com.opennxt.model.lobby

import com.opennxt.OpenNXT
import com.opennxt.model.tick.Tickable
import com.opennxt.net.ConnectedClient
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.game.serverprot.variables.ResetClientVarcache
import com.opennxt.resources.config.enums.EnumDefinition
import mu.KotlinLogging

class LobbyPlayer(var client: ConnectedClient) : Tickable {
    private val logger = KotlinLogging.logger { }

    fun handleIncomingPackets() {
        val queue = client.incomingQueue
        while (true) {
            val packet = queue.poll() ?: return

            logger.info { "todo: handle incoming $packet" }
        }
    }

    fun added() {
        client.write(ResetClientVarcache)
        client.write(IfOpenTop(906))
    }

    override fun tick() {
        // TODO Do lobby players even need to be ticked?
    }
}