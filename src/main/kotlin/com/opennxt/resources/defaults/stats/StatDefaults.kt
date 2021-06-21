package com.opennxt.resources.defaults.stats

import com.opennxt.resources.defaults.Default
import com.opennxt.resources.defaults.DefaultGroup
import io.netty.buffer.ByteBuf
import mu.KotlinLogging

class StatDefaults : Default {
    private val logger = KotlinLogging.logger{}

    override val group: DefaultGroup = DefaultGroup.STAT

    var tables: Array<StatExperienceTable> = arrayOf()
    var stats: Array<StatDefinition> = arrayOf()

    override fun decode(buf: ByteBuf) {
        while (buf.isReadable) {
            val opcode = buf.readUnsignedByte().toInt()
            if (opcode == 0) break

            when(opcode) {
                1 -> decodeStatDefinitions(buf)
                2 -> decodeExperienceTables(buf)
                else -> throw IllegalArgumentException("Unknown stat opcode: $opcode")
            }
        }

        logger.info("Decoded ${stats.size} skills using ${tables.size + 1} unique experience tables.")
    }

    private fun decodeStatDefinitions(buf: ByteBuf) {
        stats = Array(buf.readUnsignedByte().toInt()) { StatDefinition(it, StatExperienceTable.DEFAULT, 99) }

        for (i in stats.indices) {
            val id = buf.readUnsignedByte().toInt()
            val cap = buf.readUnsignedShort()
            var table = StatExperienceTable.DEFAULT

            val flags = buf.readUnsignedByte().toInt()
            if (flags and 0x2 != 0) buf.readUnsignedByte()
            if (flags and 0x4 != 0) {
                val tableId = buf.readUnsignedByte().toInt()
                if (tableId >= tables.size) {
                    logger.error("Xp table for stat '$id' out of bounds: '$tableId', using default table")
                } else {
                    table = tables[tableId]
                }
            }
            if (flags and 0x8 != 0) buf.readByte()
            buf.readByte()

            stats[id] = StatDefinition(id, table, cap)
        }
    }

    private fun decodeExperienceTables(buf: ByteBuf) {
        tables = Array(buf.readUnsignedByte().toInt()) { StatExperienceTable.DEFAULT }

        var id = buf.readUnsignedByte().toInt()
        while (id != 255) {
            val values = IntArray(buf.readUnsignedShort())
            for (level in values.indices)
                values[level] = buf.readInt()
            tables[id] = StatExperienceTable(values)
            id = buf.readUnsignedByte().toInt()
        }
    }
}