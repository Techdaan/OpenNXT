package com.opennxt.filesystem.compression

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GZIPCompression {
    fun decompress(compressed: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
    }

    fun compress(uncompressed: ByteArray): ByteArray {
        return ByteArrayInputStream(uncompressed).use { input ->
            ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { gzip ->
                    val block = ByteArray(4096)
                    while (true) {
                        val len = input.read(block)
                        if (len == -1) break

                        gzip.write(block, 0, len)
                    }
                }
                baos.toByteArray()
            }
        }
    }
}