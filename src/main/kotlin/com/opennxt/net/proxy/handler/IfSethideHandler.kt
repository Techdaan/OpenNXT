package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfSethide
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfSethideHandler : GamePacketHandler<ProxyPlayer, IfSethide> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: IfSethide) {
        context.plaintextDumpFile.appendLine("player.interfaces.hide(id = ${packet.parent.parent}, component = ${packet.parent.component}, hidden = ${packet.hidden})")
    }
}