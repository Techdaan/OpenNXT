package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfOpenSubProxyHandler : GamePacketHandler<ProxyPlayer, IfOpenSub> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: IfOpenSub) {
        context.plaintextDumpFile.appendLine("player.interfaces.open(id = ${packet.id}, parent = ${packet.parent.parent}, component = ${packet.parent.component}, walkable = ${packet.flag})")
    }
}