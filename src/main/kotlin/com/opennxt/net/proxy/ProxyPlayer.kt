package com.opennxt.net.proxy

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.handlers.ClientCheatHandler
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.proxy.handler.IfOpenSubProxyHandler
import com.opennxt.net.proxy.handler.IfOpenTopProxyHandler
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging
import kotlin.reflect.KClass

class ProxyPlayer(val proxyClient: ConnectedProxyClient) : BasePlayer(proxyClient.connection) {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<in BasePlayer, out GamePacket>>()
    private val logger = KotlinLogging.logger { }

    init {
        handlers[ClientCheat::class] = ClientCheatHandler

        handlers[IfOpenTop::class] = IfOpenTopProxyHandler
        handlers[IfOpenSub::class] = IfOpenSubProxyHandler
    }

    fun handlePacket(packet: GamePacket): Boolean {
        val handler = handlers[packet::class]
        if (handler == null) {
            logger.info { "$packet" }
            return false
        }

        try {
            (handler as GamePacketHandler<in BasePlayer, GamePacket>)?.handle(this, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }
}