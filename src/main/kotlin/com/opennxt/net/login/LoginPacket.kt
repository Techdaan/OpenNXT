package com.opennxt.net.login

import com.opennxt.model.Build
import com.opennxt.net.GenericResponse
import com.opennxt.net.IncomingPacket
import com.opennxt.net.OutgoingPacket
import io.netty.buffer.ByteBuf

sealed class LoginPacket : IncomingPacket, OutgoingPacket {
    data class SendUniqueId(val id: Long) : LoginPacket()
    class LobbyLoginRequest(
        val build: Build,
        val header: LoginRSAHeader,
        val username: String,
        val password: String,
        val remaining: ByteBuf
    ) : LoginPacket()

    class GameLoginRequest(
        val build: Build,
        val header: LoginRSAHeader,
        val username: String,
        val password: String,
        val remaining: ByteBuf
    ) : LoginPacket()

    data class GameLoginResponse(
        val byte0: Int,
        val rights: Int,
        val byte2: Int,
        val byte3: Int,
        val byte4: Int,
        val byte5: Int,
        val byte6: Int,
        val playerIndex: Int,
        val byte8: Int,
        val medium9: Int,
        val isMember: Int,
        val username: String,
        val short12: Int, // part of 6-byte-int
        val int13: Int // part of 6-byte-int
    ) : LoginPacket()

    data class LobbyLoginResponse(
        val byte0: Int,
        val rights: Int,
        val byte2: Int,
        val byte3: Int,
        val medium4: Int,
        val byte5: Int,
        val byte6: Int,
        val byte7: Int,
        val long8: Long,
        val int9: Int,
        val byte10: Int,
        val byte11: Int,
        val int12: Int,
        val int13: Int,
        val short14: Int,
        val short15: Int,
        val short16: Int,
        val ip: Int,
        val byte17: Int,
        val short18: Int,
        val short19: Int,
        val byte20: Int,
        val username: String,
        val byte22: Int,
        val int23: Int,
        val short24: Int,
        val defaultWorld: String,
        val defaultWorldPort1: Int,
        val defaultWorldPort2: Int
    ) : LoginPacket()

    data class LoginResponse(val code: GenericResponse) : LoginPacket()

    data class ServerpermVarcChunk(val finished: Boolean, val varcs: Map<Int, Any>) : LoginPacket()
}