package com.opennxt.net.proxy

import com.opennxt.net.GenericResponse
import com.opennxt.net.login.LoginPacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class LoginClientDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val state = ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).get()

        if (state == ProxyLoginState.HANDSHAKE || state == ProxyLoginState.LOGIN_RESPONSE) {
            val responseId = buf.readUnsignedByte().toInt()
            val response = GenericResponse.fromId(responseId)
                ?: throw NullPointerException("response not found: $responseId")

            out.add(LoginPacket.LoginResponse(response))
            return
        }

        if (state == ProxyLoginState.UNIQUE_ID && buf.readableBytes() >= 8) {
            out.add(LoginPacket.SendUniqueId(buf.readLong()))
            return
        }

        TODO("Not yet implemented")
    }
}