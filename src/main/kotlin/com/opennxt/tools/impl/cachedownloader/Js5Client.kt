package com.opennxt.tools.impl.cachedownloader

import com.opennxt.net.js5.packet.Js5Packet
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import java.lang.Integer.max
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Js5Client(val version: Int, val token: String) {
    companion object {
        val ATTR_KEY = AttributeKey.valueOf<Js5Client>("js5-client")
    }

    val lock = Any()
    private val connectedLatch = CountDownLatch(1)

    var channel: Channel? = null
    val downloadingHighPriority = Long2ObjectOpenHashMap<Js5RequestHandler.ArchiveRequest>()
    val downloadingLowPriority = Long2ObjectOpenHashMap<Js5RequestHandler.ArchiveRequest>()
    var current: Js5RequestHandler.ArchiveRequest? = null
    var state = Js5ClientState.HANDSHAKE
    var lastRead = System.currentTimeMillis()

    fun markAsCrashed() {
        synchronized(lock) {
            downloadingHighPriority.values.forEach { it.crashed = true }
            downloadingLowPriority.values.forEach { it.crashed = true }
        }
    }

    fun getRequest(priority: Boolean, index: Int, archive: Int): Js5RequestHandler.ArchiveRequest? {
        synchronized(lock) {
            val list = if (priority) downloadingHighPriority else downloadingLowPriority

            return list[(index.toLong() shl 32) or archive.toLong()]
        }
    }

    fun countAllowedRequests(): Int {
        val channel = channel
        if (state != Js5ClientState.ACTIVE || channel == null || !channel.isOpen)
            return 0

        val lowRequests = downloadingLowPriority.size
        val prioRequests = downloadingHighPriority.size

        return max(0, 500 - max(lowRequests, prioRequests))
    }

    fun addAllUnchecked(requests: Collection<Js5RequestHandler.ArchiveRequest>) {
        val channel = channel
        if (state != Js5ClientState.ACTIVE || channel == null || !channel.isOpen)
            return

        requests.forEach { request ->
            val list = if (request.priority) downloadingHighPriority else downloadingLowPriority

            list[(request.index.toLong() shl 32) or request.archive.toLong()] = request
            channel.writeAndFlush(Js5Packet.RequestFile(request.priority, request.index, request.archive, version))
        }
    }

    fun addRequest(request: Js5RequestHandler.ArchiveRequest): Boolean {
        val channel = channel
        if (state != Js5ClientState.ACTIVE || channel == null || !channel.isOpen)
            return false

        val list = if (request.priority) downloadingHighPriority else downloadingLowPriority

        if (list.size >= 500)
            return false

        synchronized(lock) {
            list[(request.index.toLong() shl 32) or request.archive.toLong()] = request
            channel.writeAndFlush(Js5Packet.RequestFile(request.priority, request.index, request.archive, version))
        }
        return true
    }

    // Should only be called by the decoder.
    fun removeRequest(request: Js5RequestHandler.ArchiveRequest): Boolean {
        synchronized(lock) {
            val list = if (request.priority) downloadingHighPriority else downloadingLowPriority
            return list.remove((request.index.toLong() shl 32) or request.archive.toLong()) != null
        }
    }

    fun awaitConnected(timeout: Long, timeUnit: TimeUnit): Boolean {
        return connectedLatch.await(timeout, timeUnit)
    }

    fun notifyConnected() {
        connectedLatch.countDown()
    }
}