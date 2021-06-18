package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfOpenTopProxyHandler: GamePacketHandler<ProxyPlayer, IfOpenTop> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: IfOpenTop) {
        context.plaintextDumpFile.appendLine("player.interfaces.openTop(id = ${packet.id})")
    }
}