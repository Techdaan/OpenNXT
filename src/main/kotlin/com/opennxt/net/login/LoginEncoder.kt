package com.opennxt.net.login

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class LoginEncoder: MessageToByteEncoder<LoginPacket>() {
    override fun encode(ctx: ChannelHandlerContext, msg: LoginPacket, out: ByteBuf) {
        when (msg) {
            is LoginPacket.Response -> out.writeByte(msg.code.id)
            is LoginPacket.SendUniqueId -> out.writeLong(msg.id)
            else -> TODO("Encode $msg")
        }
    }
}