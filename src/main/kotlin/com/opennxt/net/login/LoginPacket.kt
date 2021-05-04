package com.opennxt.net.login

import com.opennxt.model.Build
import com.opennxt.net.GenericResponse
import com.opennxt.net.IncomingPacket
import com.opennxt.net.OutgoingPacket
import io.netty.buffer.ByteBuf

sealed class LoginPacket: IncomingPacket, OutgoingPacket {
    data class SendUniqueId(val id: Long): LoginPacket()
    class LobbyLoginRequest(val build: Build, val header: LoginRSAHeader, val username: String, val password: String, val remaining: ByteBuf): LoginPacket()

    data class LoginResponse(val code: GenericResponse): LoginPacket()
}