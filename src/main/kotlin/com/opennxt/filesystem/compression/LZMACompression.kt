package com.opennxt.filesystem.compression

import lzma.sdk.lzma.Decoder
import lzma.sdk.lzma.Encoder
import lzma.streams.LzmaEncoderWrapper
import lzma.streams.LzmaOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object LZMACompression {
    private val LZMA_DECODER = Decoder()

    private object RSLZMAEncoder : LzmaEncoderWrapper(null) {
        val encoder: Encoder = Encoder()

        override fun code(input: InputStream?, output: OutputStream?) {
            encoder.writeCoderProperties(output)
            encoder.code(input, output, -1, -1, null)
        }
    }

    fun decompress(compressed: ByteArray, size: Int): ByteArray {
        val properties = ByteArray(5)
        val body = ByteArray(compressed.size - 5)

        System.arraycopy(compressed, 0, properties, 0, 5)
        System.arraycopy(compressed, 5, body, 0, compressed.size - 5)

        synchronized(LZMA_DECODER) {
            if (!LZMA_DECODER.setDecoderProperties(properties))
                throw IllegalStateException("Invalid LZMA decoder properties: ${Arrays.toString(properties)}")

            return ByteArrayInputStream(body).use { inStream ->
                ByteArrayOutputStream(size).use { outStream ->
                    LZMA_DECODER.code(inStream, outStream, size.toLong())
                    outStream.toByteArray()
                }
            }
        }
    }

    fun compress(uncompressed: ByteArray): ByteArray {
        synchronized(RSLZMAEncoder) {
            return ByteArrayInputStream(uncompressed).use { input ->
                ByteArrayOutputStream().use { baos ->
                    LzmaOutputStream(baos, RSLZMAEncoder).use { lzma ->
                        val block = ByteArray(4096)
                        while (true) {
                            val len = input.read(block)
                            if (len == -1) break

                            lzma.write(block, 0, len)
                        }
                    }

                    baos.toByteArray()
                }
            }
        }
    }
}