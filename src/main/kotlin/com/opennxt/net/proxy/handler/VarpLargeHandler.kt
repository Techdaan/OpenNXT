package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.variables.VarpLarge
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object VarpLargeHandler : GamePacketHandler<ProxyPlayer, VarpLarge> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: VarpLarge) {
        context.plaintextDumpFile.appendLine("channel.write(VarpLarge(${packet.id}, ${packet.value}))")
    }
}