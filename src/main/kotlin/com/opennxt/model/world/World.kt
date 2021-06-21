package com.opennxt.model.world

import com.opennxt.model.entity.EntityList
import com.opennxt.model.entity.PlayerEntity
import com.opennxt.model.lobby.LobbyPlayer
import com.opennxt.model.tick.Tickable
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

class World : Tickable {
    private val logger = KotlinLogging.logger { }

    private val playerEntities = EntityList<PlayerEntity>(2000)
    private val players = HashSet<WorldPlayer>()

    private val toAdd = ConcurrentLinkedQueue<WorldPlayer>()

    override fun tick() {
        while (true) {
            val player = toAdd.poll() ?: break
            players += player
            playerEntities.add(player.entity)
            player.added()
        }

        players.forEach { it.handleIncomingPackets() }
        players.forEach { it.tick() }
        players.forEach { it.client.flush() }
    }

    fun getPlayer(index: Int): PlayerEntity? = playerEntities[index]

    fun addPlayer(player: WorldPlayer) {
        toAdd += player
    }
}