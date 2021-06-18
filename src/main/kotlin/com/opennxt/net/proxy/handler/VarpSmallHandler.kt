package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.variables.VarpSmall
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object VarpSmallHandler : GamePacketHandler<ProxyPlayer, VarpSmall> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: VarpSmall) {
        context.plaintextDumpFile.appendLine("channel.write(VarpSmall(${packet.id}, ${packet.value}))")
    }
}