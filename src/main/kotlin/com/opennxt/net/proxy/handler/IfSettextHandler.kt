package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfSettext
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object IfSettextHandler : GamePacketHandler<ProxyPlayer, IfSettext> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: IfSettext) {
        context.plaintextDumpFile.appendLine("player.interfaces.text(id = ${packet.parent.parent}, component = ${packet.parent.component}, text = \"${packet.text.replace("\"", "\\\"")}\")")
    }
}