package com.opennxt.net.proxy

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.handlers.ClientCheatHandler
import com.opennxt.net.game.pipeline.GamePacketHandler
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.reflect.KClass

class ProxyPlayer(val proxyClient: ConnectedProxyClient): BasePlayer(proxyClient.connection) {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<in BasePlayer, out GamePacket>>()

    init {
        handlers[ClientCheat::class] = ClientCheatHandler
    }

    fun handlePacket(packet: GamePacket): Boolean {
        val handler = handlers[packet::class] ?: return false

        try {
            (handler as GamePacketHandler<in BasePlayer, GamePacket>)?.handle(this, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }
}