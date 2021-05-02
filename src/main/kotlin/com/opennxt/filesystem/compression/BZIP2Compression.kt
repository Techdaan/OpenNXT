package com.opennxt.filesystem.compression

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object BZIP2Compression {
    fun decompress(compressed: ByteArray): ByteArray {
        val fixed = ByteArray(compressed.size + 4)
        System.arraycopy(compressed, 0, fixed, 4, compressed.size)
        fixed[0] = 'B'.toByte()
        fixed[1] = 'Z'.toByte()
        fixed[2] = 'h'.toByte()
        fixed[3] = '1'.toByte()

        return BZip2CompressorInputStream(ByteArrayInputStream(fixed)).use { it.readBytes() }
    }

    fun compress(uncompressed: ByteArray): ByteArray {
        val buffer = ByteArrayInputStream(uncompressed).use { input ->
            ByteArrayOutputStream().use { baos ->
                BZip2CompressorOutputStream(baos, 1).use { bzip2 ->
                    val block = ByteArray(4096)
                    while (true) {
                        val len = input.read(block)
                        if (len == -1) break

                        bzip2.write(block, 0, len)
                    }
                }
                baos.toByteArray()
            }
        }

        // Strip the BZIP header off
        val stripped = ByteArray(buffer.size - 4)
        System.arraycopy(buffer, 4, stripped, 0, stripped.size)

        return stripped;
    }
}