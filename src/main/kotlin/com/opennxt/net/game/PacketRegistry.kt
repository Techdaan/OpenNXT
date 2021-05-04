package com.opennxt.net.game

import com.opennxt.OpenNXT
import com.opennxt.net.Side
import com.opennxt.net.game.pipeline.GamePacketCodec
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.reflect.KClass

object PacketRegistry {
    private val serverProtByOpcode = Int2ObjectOpenHashMap<Registration>()
    private val clientProtByOpcode = Int2ObjectOpenHashMap<Registration>()

    private val serverProtByClass = Object2ObjectOpenHashMap<KClass<*>, Registration>()
    private val clientProtByClass = Object2ObjectOpenHashMap<KClass<*>, Registration>()

    data class Registration(
        val name: String,
        val opcode: Int,
        val clazz: KClass<*>,
        val codec: GamePacketCodec<*>
    )

    fun <T : GamePacket> register(side: Side, name: String, clazz: KClass<T>, codec: GamePacketCodec<T>) {
        @Suppress("DEPRECATION")
        val opcode = (if (side == Side.CLIENT) OpenNXT.protocol.clientProtNames else OpenNXT.protocol.serverProtNames)
            .values[name] ?: throw NullPointerException("side $side name $name")

        val registration = Registration(name, opcode, clazz, codec)

        if (side == Side.CLIENT) {
            clientProtByClass[clazz] = registration
            clientProtByOpcode[opcode] = registration
        } else {
            serverProtByClass[clazz] = registration
            serverProtByOpcode[opcode] = registration
        }
    }

    fun reload() {
        clientProtByOpcode.clear()
        clientProtByClass.clear()
        serverProtByClass.clear()
        serverProtByOpcode.clear()

    }

    fun getRegistration(side: Side, opcode: Int): Registration? {
        return if (side == Side.CLIENT) {
            clientProtByOpcode[opcode]
        } else {
            serverProtByOpcode[opcode]
        }
    }

    fun getRegistration(side: Side, clazz: KClass<*>): Registration? {
        return if (side == Side.CLIENT) {
            clientProtByClass[clazz]
        } else {
            serverProtByClass[clazz]
        }
    }
}