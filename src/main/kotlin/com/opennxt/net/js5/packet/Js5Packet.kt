package com.opennxt.net.js5.packet

import com.opennxt.net.Packet
import io.netty.buffer.ByteBuf

sealed class Js5Packet : Packet {
    data class ConnectionInitialized(val value: Int, val build: Int) : Js5Packet()
    data class LoggedIn(val build: Int) : Js5Packet()
    data class LoggedOut(val build: Int) : Js5Packet()
    data class RequestTermination(val build: Int) : Js5Packet()
    data class RequestFile(
        var priority: Boolean,
        val index: Int,
        val archive: Int,
        val build: Int,
        var nxt: Boolean = true
    ) : Js5Packet()

    data class Handshake(val major: Int, val minor: Int, val token: String, val language: Int = 0) : Js5Packet()
    data class XorRequest(val xor: Int) : Js5Packet()
    data class RequestFileResponse(val priority: Boolean, val index: Int, val archive: Int, val data: ByteBuf) :
        Js5Packet()

    // TODO Responses here
    data class HandshakeResponse(val code: Int) : Js5Packet()
    data class Prefetches(val prefetches: IntArray) : Js5Packet()
}