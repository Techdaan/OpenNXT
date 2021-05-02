package com.opennxt.filesystem

import com.opennxt.ext.getCrc32
import com.opennxt.ext.getWhirlpool
import com.opennxt.ext.rsaEncrypt
import com.opennxt.util.Whirlpool
import io.netty.buffer.ByteBuf
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer

class ChecksumTable(val entries: Array<TableEntry>) {
    data class TableEntry(val crc: Int, val version: Int, val files: Int, val size: Int, val whirlpool: ByteArray) {
        companion object {
            val EMPTY = TableEntry(0, 0, 0, 0, ByteArray(64))
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TableEntry

            if (crc != other.crc) return false
            if (version != other.version) return false
            if (files != other.files) return false
            if (size != other.size) return false
            if (!whirlpool.contentEquals(other.whirlpool)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = crc
            result = 31 * result + version
            result = 31 * result + files
            result = 31 * result + size
            result = 31 * result + whirlpool.contentHashCode()
            return result
        }
    }

    companion object {
        fun decode(buffer: ByteBuf): ChecksumTable {
            return ChecksumTable(Array(buffer.readUnsignedByte().toInt()) { id ->
                val crc = buffer.readInt()
                val version = buffer.readInt()
                val files = buffer.readInt()
                val size = buffer.readInt()
                val whirlpool = ByteArray(64)
                buffer.readBytes(whirlpool)

                TableEntry(crc, version, files, size, whirlpool)
            })
        }

        fun decode(buffer: ByteBuffer): ChecksumTable {
            return ChecksumTable(Array(buffer.get().toInt()) { id ->
                val crc = buffer.int
                val version = buffer.int
                val files = buffer.int
                val size = buffer.int
                val whirlpool = ByteArray(64)
                buffer.get(whirlpool)

                TableEntry(crc, version, files, size, whirlpool)
            })
        }

        fun create(fs: Filesystem, http: Boolean): ChecksumTable {
            val entries = Array(if (http) 41 else fs.numIndices()) { index ->
                val raw = fs.readReferenceTable(index) ?: return@Array TableEntry.EMPTY
                val table = fs.getReferenceTable(index) ?: return@Array TableEntry.EMPTY

                if ((http && index == 40) || !http) {
                    return@Array TableEntry(
                        raw.getCrc32(),
                        table.version,
                        table.highestEntry(),
                        table.archiveSize(),
                        raw.getWhirlpool()
                    )
                }

                TableEntry.EMPTY
            }

            return ChecksumTable(entries)
        }
    }

    fun encode(modulus: BigInteger, exponent: BigInteger): ByteArray {
        val bout = ByteArrayOutputStream()
        val dos = DataOutputStream(bout)

        dos.use { os ->
            os.write(entries.size)

            for(element in entries) {
                val entry = element
                os.writeInt(entry.crc)
                os.writeInt(entry.version)
                os.writeInt(entry.files)
                os.writeInt(entry.size)
                os.write(entry.whirlpool)
            }

            val all = bout.toByteArray()
            var temp = ByteBuffer.allocate(65)
            temp.put(10)
            temp.put(Whirlpool.getHash(all, 0, all.size))
            temp.flip()
            temp = temp.rsaEncrypt(modulus, exponent)

            val bytes = ByteArray(temp.limit())
            temp.get(bytes)
            os.write(bytes)

            return bout.toByteArray()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChecksumTable

        if (!entries.contentEquals(other.entries)) return false

        return true
    }

    override fun hashCode(): Int {
        return entries.contentHashCode()
    }

}