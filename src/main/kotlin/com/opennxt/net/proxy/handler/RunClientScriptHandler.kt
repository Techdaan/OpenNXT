package com.opennxt.net.proxy.handler

import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.RunClientScript
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object RunClientScriptHandler : GamePacketHandler<ProxyPlayer, RunClientScript> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: ProxyPlayer, packet: RunClientScript) {
        if (packet.args.isEmpty()) {
            context.plaintextDumpFile.appendLine("player.client.write(RunClientScript(script = ${packet.script}))")
        } else {
            context.plaintextDumpFile.appendLine(
                "player.client.write(RunClientScript(script = ${packet.script}, args = arrayOf(${
                    packet.args.joinToString(separator = ", ") {
                        if (it is String) {
                            "\"${it.replace("\"", "\\\"")}\""
                        } else {
                            it.toString()
                        }
                    }
                })))"
            )
        }
    }
}