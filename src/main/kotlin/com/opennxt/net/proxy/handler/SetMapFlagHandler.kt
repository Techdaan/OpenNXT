package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.SetMapFlag
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object SetMapFlagHandler : GamePacketHandler<ProxyPlayer, SetMapFlag> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: SetMapFlag) {
        (context as ProxyPlayer).plaintextDumpFile.appendLine("// Set map flag $packet")
        logger.info { "Map flag: $packet" }
    }
}