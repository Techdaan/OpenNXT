package com.opennxt.net.proxy

import com.google.common.collect.Sets
import com.opennxt.model.tick.Tickable
import mu.KotlinLogging

class ProxyConnectionHandler : Tickable {
    private val logger = KotlinLogging.logger { }

    private class ProxyPair(val client: ConnectedProxyClient, val server: ConnectedProxyClient) {
        fun isAlive(): Boolean {
            return client.connection.channel.isOpen && server.connection.channel.isOpen
        }

        fun disconnect() {
            client.connection.channel.close()
            server.connection.channel.close()
        }
    }

    private val clients = Sets.newConcurrentHashSet<ProxyPair>()

    fun registerProxyConnection(client: ConnectedProxyClient, server: ConnectedProxyClient) {
        clients += ProxyPair(client, server)
    }

    override fun tick() {
        val it = clients.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (!pair.isAlive()) {
                pair.disconnect()
                it.remove()
            }

            try {
                pair.client.tick()
            } catch (e: Exception) {
                logger.error(e) { "Uncaught exception ticking client in proxy handler" }
            }

            try {
                pair.server.tick()
            } catch (e: Exception) {
                logger.error(e) { "Uncaught exception ticking server in proxy handler" }
            }

            if (pair.client.ready)
                pair.client.connection.flush()
            if (pair.server.ready)
                pair.server.connection.flush()
        }
    }
}