package com.opennxt.resources.config.structs

import com.opennxt.ext.*
import com.opennxt.filesystem.Filesystem
import com.opennxt.resources.FilesystemResourceCodec
import com.opennxt.resources.ResourceType
import com.opennxt.resources.config.vars.BaseVarType
import com.opennxt.resources.config.vars.ScriptVarType
import com.opennxt.util.TextUtils
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

object StructFilesystemCodec : FilesystemResourceCodec<StructDefinition> {
    override fun getMaxId(fs: Filesystem): Int {
        val table = fs.getReferenceTable(22) ?: return 0
        val maxArchive = table.highestEntry() - 1
        val files = table.archives[maxArchive]!!.files.lastKey()

        return maxArchive * 32 + files
    }

    override fun list(fs: Filesystem): Map<Int, StructDefinition> {
        val result = Int2ObjectAVLTreeMap<StructDefinition>()
        for (i in 0 until getMaxId(fs) + 1) {
            val def = load(fs, i) ?: continue
            result[i] = def
        }
        return result
    }

    override fun load(fs: Filesystem, id: Int): StructDefinition? {
        val table = fs.getReferenceTable(22) ?: return null
        val archive = table.loadArchive(ResourceType.getArchive(id, 5)) ?: return null
        val file = archive.files[ResourceType.getFile(id, 5)] ?: return null

        val definition = StructDefinition()
        val buffer = ByteBuffer.wrap(file.data)
        while (buffer.hasRemaining()) {
            val opcode = buffer.get().toInt() and 0xff
            if (opcode == 0) return definition

            when (opcode) {
                249 -> {
                    val size = buffer.get().toInt()
                    for(i in 0 until size) {
                        val isString = buffer.get() == 1.toByte()
                        definition.values[buffer.getMedium()] = if (isString) buffer.getString() else buffer.int
                    }
                }
                else -> throw IllegalArgumentException("invalid StructDefinition opcode $opcode")
            }
        }
        return definition
    }

    override fun store(fs: Filesystem, id: Int, data: StructDefinition) {
        val table = fs.getReferenceTable(22) ?: throw NullPointerException("index 22 table")
        val archive = table.loadOrCreateArchive(ResourceType.getArchive(id, 5)) ?:
        throw NullPointerException("failed to create or get archive ${ResourceType.getArchive(id, 5)}")

        var size = 4096
        while (true) {
            try {
                val buffer = ByteBuffer.allocate(size)

                buffer.put(249.toByte())
                buffer.put(data.values.size.toByte())
                data.values.forEach { (k, v) ->
                    if (v is String) {
                        buffer.put(1)
                        buffer.putMedium(k)
                        buffer.putString(v)
                    } else {
                        buffer.put(0)
                        buffer.putMedium(k)
                        buffer.putInt(v as Int)
                    }
                }

                buffer.put(0)

                buffer.flip()
                val encoded = buffer.toByteArray()
                archive.putFile(ResourceType.getFile(id, 8), encoded)

                return
            } catch (e: BufferOverflowException) {
                size *= 4
            }
        }
    }
}