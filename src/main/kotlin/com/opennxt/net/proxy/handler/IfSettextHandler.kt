package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.game.serverprot.ifaces.IfSetevents
import com.opennxt.net.game.serverprot.ifaces.IfSettext
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfSettextHandler : GamePacketHandler<BasePlayer, IfSettext> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: IfSettext) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("player.interfaces.text(id = ${packet.parent.parent}, component = ${packet.parent.component}, text = \"${packet.text.replace("\"", "\\\"")}\")")
    }
}