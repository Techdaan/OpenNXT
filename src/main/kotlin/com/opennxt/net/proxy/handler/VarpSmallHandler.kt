package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.variables.VarpSmall
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object VarpSmallHandler : GamePacketHandler<BasePlayer, VarpSmall> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: VarpSmall) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("channel.write(VarpSmall(${packet.id}, ${packet.value}))")
    }
}