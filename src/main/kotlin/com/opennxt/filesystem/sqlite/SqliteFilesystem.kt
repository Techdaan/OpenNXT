package com.opennxt.filesystem.sqlite

import com.opennxt.ext.toFilesystemHash
import com.opennxt.filesystem.Container
import com.opennxt.filesystem.Filesystem
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32

class SqliteFilesystem(path: Path) : Filesystem(path) {
    val logger = KotlinLogging.logger { }

    var indices: Array<SqliteIndexFile?>

    init {
        if (!Files.exists(path))
            Files.createDirectories(path)

        logger.info { "Opening SQLite filesystem from $path" }
        var count = 0
        while (count < 255 && Files.exists(path.resolve("js5-$count.jcache"))) count++
        logger.info { "Discovered a total of $count indices..." }

        indices = Array(count) { i ->
            val file = SqliteIndexFile(path.resolve("js5-$i.jcache"))
            logger.trace("Loading index $i, contains data: ${file.hasReferenceTable()} with max archive: ${file.getMaxArchive()}")
            file
//            return@Array if (file.hasReferenceTable()) file else null
        }
    }

    override fun createIndex(id: Int) {
        if (id < indices.size) {
            throw IllegalArgumentException("index $id already exists (arr size = ${indices.size})")
        }
        if (id != indices.size) {
            throw IllegalArgumentException("create indices one by one (got $id, start at ${indices.size})")
        }

        val tmp = indices
        this.indices = Array(id + 1) {
            if (it != id)
                return@Array tmp[it]
            SqliteIndexFile(path.resolve("js5-$it.jcache"))
        }
    }

    override fun exists(index: Int, archive: Int): Boolean {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        return indices[index]?.exists(archive) ?: false
    }

    override fun read(index: Int, archive: Int): ByteBuffer? {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        return ByteBuffer.wrap(indices[index]?.getRaw(archive) ?: return null)
    }

    override fun read(index: Int, name: String): ByteBuffer? {
        val table = getReferenceTable(index) ?: return null
        val hash = name.toFilesystemHash()
        val id = (table.archives.entries.firstOrNull { it.value.name == hash } ?: return null).key
        return read(index, id)
    }

    override fun readReferenceTable(index: Int): ByteBuffer? {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        return ByteBuffer.wrap(indices[index]?.getRawTable() ?: return null)
    }

    override fun write(index: Int, archive: Int, data: Container) {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        val compressed = data.compress().array()
        val crc = CRC32()
        crc.update(compressed, 0, compressed.size - 2)
        write(index, archive, compressed, data.version, crc.value.toInt())
    }

    override fun write(index: Int, archive: Int, compressed: ByteArray, version: Int, crc: Int) {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        indices[index]?.putRaw(archive, compressed, version, crc)
    }

    override fun writeReferenceTable(index: Int, data: Container) {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        if (data.version == -1) data.version = 100
        val compressed = data.compress().array()
        val crc = CRC32()
        crc.update(compressed, 0, compressed.size - 2)
        writeReferenceTable(index, compressed, data.version, crc.value.toInt())
    }

    override fun writeReferenceTable(index: Int, compressed: ByteArray, version: Int, crc: Int) {
        if (index < 0 || index >= indices.size) throw IndexOutOfBoundsException("index out of bounds: $index")

        indices[index]?.putRawTable(compressed, version, crc)
    }

    override fun numIndices(): Int = indices.size

}