package com.opennxt.resources.config.enums

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

object EnumFilesystemCodec : FilesystemResourceCodec<EnumDefinition> {
    override fun getMaxId(fs: Filesystem): Int {
        val table = fs.getReferenceTable(17) ?: return 0
        val maxArchive = table.highestEntry() - 1
        val files = table.archives[maxArchive]!!.files.lastKey()

        return maxArchive * 256 + files
    }

    override fun list(fs: Filesystem): Map<Int, EnumDefinition> {
        val result = Int2ObjectAVLTreeMap<EnumDefinition>()
        for (i in 0 until getMaxId(fs) + 1) {
            val def = load(fs, i) ?: continue
            result[i] = def
        }
        return result
    }

    override fun load(fs: Filesystem, id: Int): EnumDefinition? {
        val table = fs.getReferenceTable(17) ?: return null
        val archive = table.loadArchive(ResourceType.getArchive(id, 8)) ?: return null
        val file = archive.files[ResourceType.getFile(id, 8)] ?: return null

        val definition = EnumDefinition()
        val buffer = ByteBuffer.wrap(file.data)
        while (buffer.hasRemaining()) {
            val opcode = buffer.get().toInt() and 0xff
            if (opcode == 0) return definition

            when (opcode) {
                1 -> definition.keyType = ScriptVarType.getByChar(TextUtils.cp1252ToChar(buffer.get()))
                2 -> definition.valueType = ScriptVarType.getByChar(TextUtils.cp1252ToChar(buffer.get()))
                3 -> definition.defaultString = buffer.getString()
                4 -> definition.defaultInt = buffer.int
                5, 6, 7, 8 -> {
                    val stringValues = opcode == 5 || opcode == 7
                    if (opcode == 7 || opcode == 8)
                        buffer.skip(2) // Used for the array size, but we only use maps for enums
                    val size = buffer.short.toInt() and 0xffff
                    for (i in 0 until size) {
                        val key = if (opcode == 5 || opcode == 6) buffer.int else (buffer.short.toInt() and 0xffff)
                        val value: Any = if (stringValues) buffer.getString() else buffer.int
                        definition.values[key] = value
                    }
                }
                101 -> {
                    val type = buffer.getSmallSmartInt()
                    definition.keyType = ScriptVarType.getById(type) ?: throw NullPointerException("varType id $type")
                }
                102 -> {
                    val type = buffer.getSmallSmartInt()
                    definition.valueType = ScriptVarType.getById(type) ?: throw NullPointerException("varType id $type")
                }
                else -> throw IllegalArgumentException("invalid EnumDefinition opcode $opcode")
            }
        }
        return definition
    }

    override fun store(fs: Filesystem, id: Int, data: EnumDefinition) {
        val table = fs.getReferenceTable(17) ?: throw NullPointerException("index 17 table")
        val archive = table.loadOrCreateArchive(ResourceType.getArchive(id, 8)) ?:
        throw NullPointerException("failed to create or get archive ${ResourceType.getArchive(id, 8)}")

        var size = 4096
        while (true) {
            try {
                val buffer = ByteBuffer.allocate(size)

                buffer.put(101)
                buffer.putSmallSmartInt(data.keyType.id)

                buffer.put(102)
                buffer.putSmallSmartInt(data.valueType.id)

                if (data.defaultString != null) {
                    buffer.put(3)
                    buffer.putString(data.defaultString ?: "null")
                }

                if (data.defaultInt != 0) {
                    buffer.put(4)
                    buffer.putInt(data.defaultInt)
                }

                val strings = data.valueType.type == BaseVarType.STRING
                buffer.put(if (strings) 5 else 6)
                buffer.putShort(data.values.size.toShort())
                data.values.forEach { (key, value) ->
                    buffer.putInt(key)
                    if (strings) buffer.putString(value as String)
                    else buffer.putInt(value as Int)
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