package com.opennxt.net.game.serverprot

import com.opennxt.net.buf.DataType
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.GamePacketCodec

data class RunClientScript(val script: Int, val args: Array<Any> = emptyArray()) : GamePacket {
    object Codec : GamePacketCodec<RunClientScript> {
        override fun encode(packet: RunClientScript, buf: GamePacketBuilder) {
            val desc = String(packet.args.map { if (it is String) 's' else 'i' }.toCharArray())

            buf.putString(desc)
            for (i in desc.length - 1 downTo 0) {
                when (val it = packet.args[i]) {
                    is String -> buf.putString(it)
                    is Int -> buf.put(DataType.INT, it)
                    else -> throw IllegalArgumentException("RUNCLIENTSCRIPT only takes String and Int args, got '$it'")
                }
            }
            buf.put(DataType.INT, packet.script)
        }

        override fun decode(buf: GamePacketReader): RunClientScript {
            val desc = buf.getString()

            val args = arrayOfNulls<Any>(desc.length)
            val chars = desc.toCharArray()
            for (i in desc.length - 1 downTo 0) {
                if (chars[i] == 's') args[i] = buf.getString()
                else args[i] = buf.getSigned(DataType.INT).toInt()
            }

            return RunClientScript(buf.getSigned(DataType.INT).toInt(), args.requireNoNulls())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RunClientScript

        if (script != other.script) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = script
        result = 31 * result + args.contentHashCode()
        return result
    }

    override fun toString(): String = "RunClientScript(script=$script, args=[${
        args.joinToString(separator = ", ") { if (it is String) "\"$it\"" else it.toString() }
    }])"
}