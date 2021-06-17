package com.opennxt.net.proxy.handler

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.RunClientScript
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.proxy.ProxyPlayer
import mu.KotlinLogging

object RunClientScriptHandler : GamePacketHandler<BasePlayer, RunClientScript> {
    val logger = KotlinLogging.logger {}

    override fun handle(context: BasePlayer, packet: RunClientScript) {
        if (packet.args.isEmpty()) {
            (context as ProxyPlayer).plaintextDumpFile.appendLine("player.client.write(RunClientScript(script = ${packet.script}))")
        } else {
            (context as ProxyPlayer).plaintextDumpFile.appendLine(
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