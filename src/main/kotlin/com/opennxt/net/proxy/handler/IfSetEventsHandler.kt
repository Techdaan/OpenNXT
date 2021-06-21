package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfSetevents
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfSetEventsHandler : GamePacketHandler<ProxyPlayer, IfSetevents> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: IfSetevents) {
        context.plaintextDumpFile.appendLine("player.interfaces.events(id = ${packet.parent.parent}, component = ${packet.parent.component}, from = ${packet.fromSlot}, to = ${packet.toSlot}, mask = ${packet.mask})")
    }
}