package com.opennxt.resources.defaults.wearpos

import com.opennxt.resources.defaults.Default
import com.opennxt.resources.defaults.DefaultGroup
import io.netty.buffer.ByteBuf
import mu.KotlinLogging

data class WearposDefaults(
    var lefthand: Short = 0,
    var righthand: Short = 0,
    var slots: IntArray = intArrayOf(),
    var unknown1: IntArray = intArrayOf(),
    var unknown2: IntArray = intArrayOf()
) : Default {
    private val logger = KotlinLogging.logger {}

    override val group: DefaultGroup = DefaultGroup.WEARPOS

    override fun decode(buf: ByteBuf) {
        while (buf.isReadable) {
            val opcode = buf.readUnsignedByte().toInt()
            if (opcode == 0) break

            when (opcode) {
                1 -> slots = buf.readArray()
                3 -> lefthand = buf.readUnsignedByte()
                4 -> righthand = buf.readUnsignedByte()
                5 -> unknown1 = buf.readArray()
                6 -> unknown2 = buf.readArray()
                else -> throw IllegalArgumentException("Unknown wearpos opcode: $opcode")
            }
        }

        if (slots.isEmpty()) {
            logger.error {"WearposDefaults didn't load slots. This is very bad, as cache reading most likely failed."}
            throw IllegalStateException("WearposDefaults 'slots.size' == 0 after 'decode'")
        }
    }

    private fun ByteBuf.readArray(): IntArray {
        val size = readUnsignedByte().toInt()
        val array = IntArray(size)
        for (i in 0 until size)
            array[i] = readUnsignedByte().toInt()
        return array
    }
}