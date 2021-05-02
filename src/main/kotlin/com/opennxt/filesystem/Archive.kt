package com.opennxt.filesystem

import com.opennxt.ext.toByteArray
import java.nio.ByteBuffer

class Archive(
    val id: Int,
    var name: Int = 0
) {
    var crc = 0
    var version = 0
    var whirlpool: ByteArray? = null
    var uncompressedSize = 0
    var compressedSize = 0
    var hash = 0

    internal var loaded = false
    internal var requiresUpdate = false
    internal var files = sortedMapOf<Int, ArchiveFile>()

    fun putFile(id: Int, data: ByteArray) {
        requiresUpdate = true
        files[id] = ArchiveFile(id, data)
    }

    fun putFile(file: ArchiveFile) {
        requiresUpdate = true
        files[file.id] = file
    }

    fun decode(buffer: ByteBuffer) {
        loaded = true
        if (files.size == 1) {
            files[files.firstKey()]!!.data = buffer.toByteArray()
            return
        }

        buffer.position(buffer.limit() - 1)
        val numChunks = buffer.get().toInt() and 0xff

        val size = files.size
        val chunkSizes = Array(numChunks) { IntArray(size) }
        val sizes = IntArray(size)
        val offsets = IntArray(size)
        val ids = files.keys.sorted().toIntArray()

        buffer.position(buffer.limit() - 1 - numChunks * (size * 4))
        for (chunk in 0 until numChunks) {
            var chunkSize = 0
            for (file in 0 until size) {
                val delta = buffer.int
                chunkSize += delta

                chunkSizes[chunk][file] = chunkSize
                sizes[file] += chunkSize
            }
        }

        for (file in 0 until size) {
            files[ids[file]]!!.data = ByteArray(sizes[file])
        }

        buffer.position(0)

        for (chunk in 0 until numChunks) {
            for (file in 0 until size) {
                val chunkSize = chunkSizes[chunk][file]

                val offset = offsets[file]
                buffer.get(files[ids[file]]!!.data, offset, chunkSize)
                offsets[file] += chunkSize
            }
        }
    }

    fun encode(): ByteBuffer {
        var size = 0
        files.values.forEach { size += it.data.size }
        val buffer = ByteBuffer.allocate(1 + size + files.size * 4)
        if (files.size == 1) {
            return ByteBuffer.wrap(files[files.firstKey()]?.data ?: ByteArray(0))
        } else {
            var last = 0
            files.forEach {
                if (last > it.key) throw IllegalStateException("out of order $last, ${it.key}")
                buffer.put(it.value.data)
                last = it.key
            }
            val filesArray = files.values.toTypedArray()
            for (i in filesArray.indices) {
                val file = filesArray[i]
                val fileSize = file.data.size
                val previousSize = if (i == 0) 0 else filesArray[i - 1].data.size
                buffer.putInt(fileSize - previousSize)
            }
            buffer.put(1)
        }
        buffer.flip()
        return buffer
    }
}