package com.opennxt.net.js5

import com.opennxt.Js5Thread
import com.opennxt.OpenNXT
import com.opennxt.filesystem.Filesystem
import com.opennxt.net.js5.packet.Js5Packet
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedQueue

class Js5Session(val channel: Channel) : AutoCloseable {

    private val logger = KotlinLogging.logger { }

    companion object {
        val ATTR_KEY = AttributeKey.valueOf<Js5Session>("js5-session")
        val XOR_KEY = AttributeKey.valueOf<Int>("js5-xor-key")
        val LOGGED_IN = AttributeKey.valueOf<Boolean>("js5-logged-in")
    }

    val highPriorityRequests = ConcurrentLinkedQueue<Js5Packet.RequestFile>()
    val lowPriorityRequests = ConcurrentLinkedQueue<Js5Packet.RequestFile>()

    var initialized = false

    init {
        channel.attr(ATTR_KEY).set(this)
        channel.attr(XOR_KEY).set(0)
        channel.attr(LOGGED_IN).set(false)
    }

    private fun Js5Packet.RequestFile.loadFileData(): ByteBuf? {
        try {
            return when {
                index == 255 && archive == 255 -> Unpooled.wrappedBuffer(OpenNXT.checksumTable)
                index == 255 -> Unpooled.wrappedBuffer(OpenNXT.filesystem.readReferenceTable(archive) ?: return null)
                else -> Unpooled.wrappedBuffer(OpenNXT.filesystem.read(index, archive) ?: return null)
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun process(limit: Int): Int {
        var bytesSent = 0

        try {
            while (bytesSent < limit && highPriorityRequests.isNotEmpty()) {
                val request = highPriorityRequests.poll()
                val data = request.loadFileData()
                if (data == null) {
                    logger.warn { "Received file request for non-existing file: [${request.index}, ${request.archive}]" }
                } else {
                    channel.write(Js5Packet.RequestFileResponse(true, request.index, request.archive, data))
                    bytesSent += data.capacity()
                }
            }

            while (bytesSent < limit && lowPriorityRequests.isNotEmpty()) {
                val request = lowPriorityRequests.poll()
                val data = request.loadFileData()
                if (data == null) {
                    logger.warn { "Received file request for non-existing file: [${request.index}, ${request.archive}]" }
                } else {
                    channel.write(Js5Packet.RequestFileResponse(false, request.index, request.archive, data))
                    bytesSent += data.capacity()
                }
            }
        } finally {
            if (bytesSent != 0) {
                channel.flush()
            }
        }

        return bytesSent
    }

    fun initialize() {
        if (initialized) {
            logger.warn("Tried initializing js5 session twice from ${channel.remoteAddress()}. Terminating connection.")
            close()
            return
        }

        initialized = true
        Js5Thread.addSession(this)
    }

    override fun close() {
        initialized = false

        Js5Thread.addSession(this)

        channel.close()
        highPriorityRequests.clear()
        lowPriorityRequests.clear()
    }
}