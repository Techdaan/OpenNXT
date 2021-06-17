package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.*
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfSethideHandler : GamePacketHandler<BasePlayer, IfSethide> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: IfSethide) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("player.interfaces.hide(id = ${packet.parent.parent}, component = ${packet.parent.component}, hidden = ${packet.hidden})")
    }
}