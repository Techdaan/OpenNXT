package com.opennxt.filesystem

import com.opennxt.filesystem.compression.BZIP2Compression
import com.opennxt.filesystem.compression.ContainerCompression
import com.opennxt.filesystem.compression.GZIPCompression
import com.opennxt.filesystem.compression.LZMACompression
import java.nio.ByteBuffer

class Container(
    var data: ByteArray,
    var compression: ContainerCompression = ContainerCompression.LZMA,
    var version: Int = -1
) {
    companion object {
        fun decode(data: ByteBuffer): Container {
            if (!data.hasRemaining()) throw IllegalArgumentException("Provided non-readable (empty?) buffer")

            val compression = ContainerCompression.of(data.get().toInt())
            val size = data.int

            val decompressedSize = if (compression == ContainerCompression.NONE) 0 else data.int
            val compressed = ByteArray(size)
            data.get(compressed)
            val version = if (data.remaining() >= 2) data.short.toInt() and 0xffff else -1

            val decompressed = when (compression) {
                ContainerCompression.NONE -> compressed
                ContainerCompression.BZIP2 -> BZIP2Compression.decompress(compressed)
                ContainerCompression.GZIP -> GZIPCompression.decompress(compressed)
                ContainerCompression.LZMA -> LZMACompression.decompress(compressed, decompressedSize)
            }

            return Container(decompressed, compression, version)
        }

        fun wrap(data: ByteBuffer): ByteBuffer {
            val buf = ByteBuffer.allocate(5 + data.remaining())

            buf.put(ContainerCompression.NONE.id.toByte())
            buf.putInt(data.remaining())
            buf.put(data)
            buf.flip()

            return buf
        }

        fun wrap(data: ByteArray): ByteBuffer {
            val buf = ByteBuffer.allocate(5 + data.size)

            buf.put(ContainerCompression.NONE.id.toByte())
            buf.putInt(data.size)
            buf.put(data)
            buf.flip()

            return buf
        }
    }

    fun compress(): ByteBuffer {
        val compressed = when (compression) {
            ContainerCompression.NONE -> data
            ContainerCompression.BZIP2 -> BZIP2Compression.compress(data)
            ContainerCompression.GZIP -> GZIPCompression.compress(data)
            ContainerCompression.LZMA -> LZMACompression.compress(data)
        }

        val buffer =
            ByteBuffer.allocate(compressed.size + 1 + 4 + (if (compression != ContainerCompression.NONE) 4 else 0) + (if (version != -1) 2 else 0))
        buffer.put(compression.id.toByte())
        buffer.putInt(compressed.size)
        if (compression != ContainerCompression.NONE)
            buffer.putInt(data.size)
        buffer.put(compressed)
        if (version != -1)
            buffer.putShort(version.toShort())
        buffer.flip()

        return buffer
    }
}