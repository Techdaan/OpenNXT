package com.opennxt.net.login

import com.opennxt.model.Build
import com.opennxt.net.GenericResponse
import com.opennxt.net.IncomingPacket
import com.opennxt.net.OutgoingPacket
import io.netty.buffer.ByteBuf

sealed class LoginPacket: IncomingPacket, OutgoingPacket {
    class Response(val code: GenericResponse): LoginPacket()
    class LobbyLoginRequest(val build: Build, val header: LoginRSAHeader, val username: String, val password: String, val remaining: ByteBuf): LoginPacket()

    class SendUniqueId(val id: Long): LoginPacket()
}