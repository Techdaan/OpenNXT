package com.opennxt.net.game.handlers

import com.opennxt.model.lobby.LobbyPlayer
import com.opennxt.net.game.clientprot.WorldlistFetch
import com.opennxt.net.game.pipeline.GamePacketHandler

object WorldlistFetchHandler: GamePacketHandler<LobbyPlayer, WorldlistFetch> {
    override fun handle(context: LobbyPlayer, packet: WorldlistFetch) {
        context.worldList.handleRequest(packet.checksum, context.client)
    }
}