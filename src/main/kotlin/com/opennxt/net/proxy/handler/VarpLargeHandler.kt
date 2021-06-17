package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.variables.VarpLarge
import com.opennxt.net.game.serverprot.variables.VarpSmall
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object VarpLargeHandler : GamePacketHandler<BasePlayer, VarpLarge> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: VarpLarge) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("channel.write(VarpLarge(${packet.id}, ${packet.value}))")
    }
}