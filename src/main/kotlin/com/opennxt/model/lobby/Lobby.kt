package com.opennxt.model.lobby

import com.opennxt.model.tick.Tickable
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

class Lobby: Tickable {
    private val logger = KotlinLogging.logger {  }

    private val players = HashSet<LobbyPlayer>()
    private val toAdd = ConcurrentLinkedQueue<LobbyPlayer>()

    override fun tick() {
        while (true) {
            val player = toAdd.poll() ?: break
            players += player
            player.added()
        }

        players.forEach { it.handleIncomingPackets() }
        players.forEach { it.tick() }
        players.forEach { it.client.flush() }
    }

    fun addPlayer(player: LobbyPlayer) {
        toAdd += player
    }
}