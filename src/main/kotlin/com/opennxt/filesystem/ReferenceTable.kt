package com.opennxt.filesystem

import com.opennxt.ext.getSmartInt
import com.opennxt.ext.putSmartInt
import com.opennxt.ext.toByteArray
import com.opennxt.filesystem.compression.ContainerCompression
import com.opennxt.util.Whirlpool
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.util.zip.CRC32

class ReferenceTable(val filesystem: Filesystem, val index: Int) {

    val logger = KotlinLogging.logger { }

    var version = 0
    var format = 7
    var mask = 0
    internal var archives = sortedMapOf<Int, Archive>()

    fun update() {
        val toUpdate = archives.values.filter { it.requiresUpdate }
        toUpdate.forEach { archive ->
            archive.version++
            archive.requiresUpdate = false
            val container = Container(archive.encode().array(), ContainerCompression.LZMA, archive.version)
            val compressed = container.compress().array()
            val crc = CRC32()
            crc.update(compressed, 0, compressed.size - 2)
            archive.crc = crc.value.toInt()
            filesystem.write(index, archive.id, compressed, archive.version, archive.crc)

            if (mask and 0x2 != 0) {
                archive.whirlpool = Whirlpool.getHash(compressed, 0, compressed.size - 2)
            }

            logger.trace("Updating archive $index:${archive.id}")
        }

        if (toUpdate.isNotEmpty()) {
            bumpVersion()
        }
    }

    internal fun bumpVersion() {
        version++
        val data = Container(encode().toByteArray(), ContainerCompression.GZIP)
        val compressed = data.compress().array()
        val crc = CRC32()
        crc.update(compressed)
        filesystem.writeReferenceTable(index, compressed, version, crc.value.toInt())

        logger.trace("Updating reference table of index $index")
    }

    fun encode(): ByteBuffer {
        val buffer = ByteBuffer.allocate(4_000_000) // TODO Is this ever enough? Or not?
        buffer.put(format.toByte())
        if (format >= 6) buffer.putInt(version)
        buffer.put(mask.toByte()) // TODO Generate hash based on files

        val hasNames = mask and 0x1 != 0
        val hasWhirlpools = mask and 0x2 != 0
        val hasSizes = mask and 0x4 != 0
        val hasHashes = mask and 0x8 != 0

        val writeFormatInt: (Int) -> Unit = { value ->
            if (format >= 7) {
                buffer.putSmartInt(value)
            } else {
                buffer.putShort(value.toShort())
            }
        }

        val archiveIds = archives.keys.sorted().toIntArray()

        writeFormatInt(archives.size)
        for (i in archiveIds.indices) {
            writeFormatInt(archiveIds[i] - if (i == 0) 0 else archiveIds[i - 1])
        }

        if (hasNames) {
            archiveIds.forEach {
                buffer.putInt((archives[it] ?: throw IllegalStateException("invalid archive id $it")).name)
            }
        }

        archiveIds.forEach {
            buffer.putInt((archives[it] ?: throw IllegalStateException("invalid archive id $it")).crc)
        }

        if (hasHashes) {
            archiveIds.forEach {
                buffer.putInt((archives[it] ?: throw IllegalStateException("invalid archive id $it")).hash)
            }
        }

        if (hasWhirlpools) {
            archiveIds.forEach {
                buffer.put((archives[it] ?: throw IllegalStateException("invalid archive id $it")).whirlpool)
            }
        }

        if (hasSizes) {
            archiveIds.forEach {
                val archive = archives[it] ?: throw IllegalStateException("invalid archive id $it")

                buffer.putInt(archive.compressedSize)
                buffer.putInt(archive.uncompressedSize)
            }
        }

        archiveIds.forEach {
            buffer.putInt((archives[it] ?: throw IllegalStateException("invalid archive id $it")).version)
        }

        val archiveFileIds = Array(archives.size) {
            (archives[archiveIds[it]] ?: throw IllegalStateException("invalid archive id $it")).files.keys
                .sorted()
                .toIntArray()
        }

        archiveIds.forEach {
            writeFormatInt((archives[it] ?: throw IllegalStateException("invalid archive id $it")).files.size)
        }

        for (i in archiveIds.indices) {
            val fileIds = archiveFileIds[i]
            for (j in fileIds.indices) {
                writeFormatInt(fileIds[j] - if (j == 0) 0 else fileIds[j - 1])
            }
        }

        if (hasNames) {
            for (i in archiveIds.indices) {
                val archive =
                    archives[archiveIds[i]] ?: throw IllegalStateException("invalid archive id ${archiveIds[i]}")
                val fileIds = archiveFileIds[i]
                for (j in fileIds.indices) {
                    buffer.putInt(
                        (archive.files[fileIds[j]]
                            ?: throw IllegalStateException("invalid file id ${archiveIds[i]}:${fileIds[j]}")).name
                    )
                }
            }
        }

        buffer.flip()
        return buffer
    }

    fun decode(buffer: ByteBuffer) {
        format = buffer.get().toInt()
        if (format !in 5..7)
            throw IllegalArgumentException("reference table format not in range 5..7")
        version = if (format >= 6) buffer.int else 0
        mask = buffer.get().toInt()

        val hasNames = mask and 0x1 != 0
        val hasWhirlpools = mask and 0x2 != 0
        val hasSizes = mask and 0x4 != 0
        val hasHashes = mask and 0x8 != 0

        val readFormatInt: () -> Int = if (format >= 7) {
            { buffer.getSmartInt() }
        } else {
            { buffer.short.toInt() and 0xffff }
        }

        val archiveIds = IntArray(readFormatInt())
        for (i in archiveIds.indices) {
            val archiveId = readFormatInt() + if (i == 0) 0 else archiveIds[i - 1]
            archiveIds[i] = archiveId
            archives[archiveId] = Archive(archiveId)
        }

        if (hasNames) {
            archiveIds.forEach {
                (archives[it] ?: throw IllegalStateException("invalid archive id $it")).name = buffer.int
            }
        }

        archiveIds.forEach {
            (archives[it] ?: throw IllegalStateException("invalid archive id $it")).crc = buffer.int
        }

        if (hasHashes) {
            archiveIds.forEach {
                (archives[it] ?: throw IllegalStateException("invalid archive id $it")).hash = buffer.int
            }
        }

        if (hasWhirlpools) {
            archiveIds.forEach {
                val whirlpool = ByteArray(64)
                buffer.get(whirlpool)

                (archives[it] ?: throw IllegalStateException("invalid archive id $it")).whirlpool = whirlpool
            }
        }

        if (hasSizes) {
            archiveIds.forEach {
                val archive = archives[it] ?: throw IllegalStateException("invalid archive id $it")

                archive.compressedSize = buffer.int
                archive.uncompressedSize = buffer.int
            }
        }

        archiveIds.forEach {
            (archives[it] ?: throw IllegalStateException("invalid archive id $it")).version = buffer.int
        }

        val archiveFileIds = Array(archives.size) { IntArray(readFormatInt()) }

        for (i in archiveIds.indices) {
            val archive = archives[archiveIds[i]]
                ?: throw IllegalStateException("invalid archive id ${archiveIds[i]}")
            val fileIds = archiveFileIds[i]
            var fileId = 0
            for (j in fileIds.indices) {
                fileId += readFormatInt()
                archive.files[fileId] = ArchiveFile(fileId)
                fileIds[j] = fileId
            }
        }

        if (hasNames) {
            for (i in archiveIds.indices) {
                val archive = archives[archiveIds[i]]
                    ?: throw IllegalStateException("invalid archive id ${archiveIds[i]}")
                val fileIds = archiveFileIds[i]
                for (j in fileIds.indices) {
                    archive.files[fileIds[j]]!!.name = buffer.int
                }
            }
        }
    }

    fun highestEntry(): Int = if (archives.isEmpty()) 0 else archives.lastKey() + 1

    fun archiveSize(): Int {
        if (mask and 0x4 != 0) {
            var sum = 0
            for (value in archives.values)
                sum += value.uncompressedSize
            return sum
        } else {
            var sum = 0
            for (key in archives.keys) {
                val data = filesystem.read(index, key) ?: throw NullPointerException("$index, $key")
                val container = Container.decode(data)
                sum += container.data.size
            }
            return sum
        }
    }

    fun totalCompressedSize(): Long {
        var sum = 0L
        for (value in archives.values)
            sum += value.compressedSize
        return sum
    }

    fun loadArchive(id: Int): Archive? {
        val archive = archives[id] ?: return null
        if (archive.loaded) return archive

        val raw = filesystem.read(index, id) ?: return null
        val file = ByteBuffer.wrap(Container.decode(raw).data)
        archive.decode(file)

        return archive
    }

    /**
     * If the archive theoretically exists but cannot be loaded this returns null.
     */
    fun loadOrCreateArchive(id: Int): Archive? {
        val archive = archives[id]
        if (archive == null) {
            val toReturn = Archive(id)
            toReturn.requiresUpdate = true
            return toReturn
        }
        if (archive.loaded) return archive

        val raw = filesystem.read(index, id) ?: return null
        val file = ByteBuffer.wrap(Container.decode(raw).data)
        archive.decode(file)

        return archive
    }

}