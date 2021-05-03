package com.opennxt.net.js5

import com.opennxt.net.js5.packet.Js5Packet
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

class Js5Session(val channel: Channel) : AutoCloseable {

    private val logger = KotlinLogging.logger {  }

    companion object {
        val ATTR_KEY = AttributeKey.valueOf<Js5Session>("js5-session")
        val XOR_KEY = AttributeKey.valueOf<Int>("js5-xor-key")
    }

    val highPriorityRequests = ConcurrentLinkedQueue<Js5Packet.RequestFile>()
    val lowPriorityRequests = ConcurrentLinkedQueue<Js5Packet.RequestFile>()

    var initialized = false

    init {
        channel.attr(ATTR_KEY).set(this)
        channel.attr(XOR_KEY).set(0)
    }

    fun initialize() {
        if (initialized) {
            logger.warn("Tried initializing js5 session twice from ${channel.remoteAddress()}. Terminating connection.")
            close()
            return
        }

        initialized = true

        // TODO Add to js5 worker
    }

    override fun close() {
        initialized = false

        // TODO Remove from js5 worker

        channel.close()
        highPriorityRequests.clear()
        lowPriorityRequests.clear()
    }
}