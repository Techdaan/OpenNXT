package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfOpenSubProxyHandler: GamePacketHandler<BasePlayer, IfOpenSub> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: IfOpenSub) {
        logger.info { "IfOpenSub $packet" }
    }
}