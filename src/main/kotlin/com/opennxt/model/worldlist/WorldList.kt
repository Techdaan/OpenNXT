package com.opennxt.model.worldlist

import com.opennxt.ext.readNullCircumfixedString
import com.opennxt.ext.readSmartShort
import com.opennxt.ext.writeNullCircumfixedString
import com.opennxt.ext.writeSmartShort
import com.opennxt.net.ConnectedClient
import com.opennxt.net.game.serverprot.WorldListFetchReply
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import mu.KotlinLogging
import kotlin.math.min

class WorldList(var entries: Array<WorldListEntry> = emptyArray()) {

    private val logger = KotlinLogging.logger { }

    // updates the previous data if this returns true
    var previousPartialHash = 0
    private fun requiresFullUpdate(): Boolean {
        var partialHash = 1

        entries.forEach { partialHash = partialHash * 31 + (it.partialHash) }

        if (partialHash == previousPartialHash) return false

        previousPartialHash = partialHash
        return true
    }

    // Only used during decoding
    var buffer: ByteBuf? = null
    var lowestEntry = 0
    var highestEntry = 0
    var entryCount = 0
    var unsortedEntries: Array<WorldListEntry?> = emptyArray()
    var checksum = 0
    // End of decode vars

    private fun decodeBody(data: ByteBuf) {
        val locations = Array(data.readSmartShort()) {
            WorldListLocation(data.readSmartShort(), data.readNullCircumfixedString())
        }

        lowestEntry = data.readSmartShort()
        highestEntry = data.readSmartShort()
        entryCount = data.readSmartShort()

        unsortedEntries = arrayOfNulls(highestEntry - lowestEntry + 1)

        for (i in 0 until entryCount) {
            val id = data.readSmartShort()

            val location = locations[data.readUnsignedByte().toInt()]
            val flags = data.readInt()
            val overrideFlag = data.readSmartShort()
            val overrideActivity =
                if (overrideFlag != 0) WorldListLocation(overrideFlag, data.readNullCircumfixedString()) else null
            val activity = data.readNullCircumfixedString()
            val host = data.readNullCircumfixedString()

            val entry = WorldListEntry(id + lowestEntry, location, flags, overrideActivity, activity, host)
            unsortedEntries[id] = entry
        }

        checksum = data.readInt()
    }

    private fun updateBody(data: ByteBuf) {
        for (i in 0 until entryCount) {
            val id = data.readSmartShort()
            val playercount = data.readUnsignedShort()
            unsortedEntries[id]?.playercount = if (playercount == 65535) -1 else playercount
        }
    }

    private fun decode(data: ByteBuf) {
        if (data.readUnsignedByte().toInt() != 2)
            return

        if (data.readUnsignedByte().toInt() == 1)
            decodeBody(data)

        updateBody(data)

        if (data.isReadable) {
            logger.warn { "Bytes were still readable after decoding and updating world list body: ${data.readableBytes()} bytes" }
            return
        }

        var i = 0
        val values = arrayOfNulls<WorldListEntry>(entryCount)
        for (j in lowestEntry..highestEntry) {
            val entry = unsortedEntries[j - lowestEntry]
            if (entry != null)
                values[i++] = entry
        }

        entries = values.requireNoNulls()
    }

    fun handle(chunk: WorldListFetchReply) {
        if (buffer == null) buffer = Unpooled.buffer(20_000)
        buffer!!.writeBytes(chunk.chunk)

        if (chunk.lastChunk) {
            ByteBufUtil.prettyHexDump(buffer!!)
            decode(buffer!!)

            buffer?.release()
            buffer = null
        }
    }

    fun handleRequest(oldChecksum: Int, output: ConnectedClient) {
        val checksum = this.hashCode()
        if (checksum == oldChecksum) {
            output.write(WorldListFetchReply(true, byteArrayOf(0)))
            return
        }

        val requiresFullUpdate = requiresFullUpdate()
        val buffer = Unpooled.buffer(if (requiresFullUpdate) 1000 else 30)

        buffer.writeByte(2) // update required
        buffer.writeByte(if (requiresFullUpdate) 1 else 0) // body requires an update

        if (requiresFullUpdate) {
            // Count unique locations
            val uniqueLocations = ObjectOpenHashSet<WorldListLocation>()
            entries.forEach { uniqueLocations.add(it.location) }
            buffer.writeSmartShort(uniqueLocations.size)

            // Write the unique locations
            val reverse = Object2IntOpenHashMap<WorldListLocation>()
            var offset = 0
            entries.forEach { entry ->
                if (reverse.containsKey(entry.location)) return@forEach

                reverse[entry.location] = offset++
                buffer.writeSmartShort(entry.location.flag)
                buffer.writeNullCircumfixedString(entry.location.name)
            }

            // Create entries map
            var min = if (entries.isEmpty()) 0 else Int.MAX_VALUE
            var max = if (entries.isEmpty()) 0 else Int.MIN_VALUE
            entries.forEach {
                if (it.id < min) min = it.id
                if (it.id > max) max = it.id
            }
            min = min(0, min)

            buffer.writeSmartShort(min)
            buffer.writeSmartShort(max)
            buffer.writeSmartShort(entries.size)

            entries.forEach { entry ->
                buffer.writeSmartShort(entry.id)
                buffer.writeByte(reverse.getValue(entry.location))
                buffer.writeInt(entry.flag)
                val override = entry.locationOverride
                if (override != null) {
                    buffer.writeSmartShort(override.flag)
                    buffer.writeNullCircumfixedString(override.name)
                } else {
                    buffer.writeSmartShort(0)
                }
                buffer.writeNullCircumfixedString(entry.activity)
                buffer.writeNullCircumfixedString(entry.host)
            }

            buffer.writeInt(checksum)
        }

        entries.forEachIndexed { _, entry ->
            buffer.writeSmartShort(entry.id)
            buffer.writeShort(entry.playercount)
        }

        while (buffer.isReadable) {
            val chunkSize = if (buffer.readableBytes() > 2999) 2999 else buffer.readableBytes()

            val chunk = ByteArray(chunkSize)
            buffer.readBytes(chunk)

            output.write(WorldListFetchReply(!buffer.isReadable, chunk))
        }
    }

    override fun toString(): String {
        return "WorldList(entries=${entries.contentToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorldList

        if (!entries.contentEquals(other.entries)) return false

        return true
    }

    override fun hashCode(): Int {
        return entries.contentHashCode()
    }

}