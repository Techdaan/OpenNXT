package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfOpenTopProxyHandler: GamePacketHandler<BasePlayer, IfOpenTop> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: IfOpenTop) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("player.interfaces.openTop(id = ${packet.id})")
    }
}