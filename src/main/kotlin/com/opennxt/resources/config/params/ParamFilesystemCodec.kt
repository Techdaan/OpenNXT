package com.opennxt.resources.config.params

import com.opennxt.ext.*
import com.opennxt.filesystem.Filesystem
import com.opennxt.resources.FilesystemResourceCodec
import com.opennxt.resources.config.vars.ScriptVarType
import com.opennxt.util.TextUtils
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

object ParamFilesystemCodec : FilesystemResourceCodec<ParamDefinition> {
    override fun getMaxId(fs: Filesystem): Int {
        val table = fs.getReferenceTable(2) ?: return 0
        return table.archives[11]!!.files.lastKey()
    }

    override fun list(fs: Filesystem): Map<Int, ParamDefinition> {
        val result = Int2ObjectAVLTreeMap<ParamDefinition>()
        for (i in 0 until getMaxId(fs) + 1) {
            val def = load(fs, i) ?: continue
            result[i] = def
        }
        return result
    }

    override fun load(fs: Filesystem, id: Int): ParamDefinition? {
        val table = fs.getReferenceTable(2) ?: return null
        val archive = table.loadArchive(11) ?: return null
        val file = archive.files[id] ?: return null

        val definition = ParamDefinition()
        val buffer = ByteBuffer.wrap(file.data)
        while (buffer.hasRemaining()) {
            val opcode = buffer.get().toInt() and 0xff
            if (opcode == 0) return definition

            when (opcode) {
                1 -> definition.type = ScriptVarType.getByChar(TextUtils.cp1252ToChar(buffer.get()))
                2 -> definition.defaultInt = buffer.int
                4 -> definition.membersOnly = true
                5 -> definition.defaultString = buffer.getString()
                101 -> {
                    val type = buffer.getSmallSmartInt()
                    definition.type = ScriptVarType.getById(type) ?: throw NullPointerException("varType id $type")
                }
                else -> throw IllegalArgumentException("invalid ParamDefinition opcode $opcode")
            }
        }
        return definition
    }

    override fun store(fs: Filesystem, id: Int, data: ParamDefinition) {
        val table = fs.getReferenceTable(2) ?: throw NullPointerException("index 2 table")
        val archive = table.loadOrCreateArchive(11) ?:
        throw NullPointerException("failed to create or get archive 11")

        var size = 4096
        while (true) {
            try {
                val buffer = ByteBuffer.allocate(size)

                buffer.put(101)
                buffer.putSmallSmartInt(data.type.id)

                if (data.defaultInt != 0) {
                    buffer.put(2)
                    buffer.putInt(data.defaultInt)
                }

                if (data.membersOnly) {
                    buffer.put(4)
                }

                if (data.defaultString != "null") {
                    buffer.put(5)
                    buffer.putString(data.defaultString)
                }

                buffer.put(0)

                buffer.flip()
                val encoded = buffer.toByteArray()
                archive.putFile(id, encoded)

                return
            } catch (e: BufferOverflowException) {
                size *= 4
            }
        }
    }
}