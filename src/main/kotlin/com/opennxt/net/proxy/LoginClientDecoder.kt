package com.opennxt.net.proxy

import com.opennxt.ext.readNullCircumfixedString
import com.opennxt.net.GenericResponse
import com.opennxt.net.login.LoginPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import mu.KotlinLogging

class LoginClientDecoder : ByteToMessageDecoder() {

    private val logger = KotlinLogging.logger { }

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val state = ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).get()

        if (state == ProxyLoginState.HANDSHAKE || state == ProxyLoginState.LOGIN_RESPONSE || state == ProxyLoginState.WAITING_WORLDLOGIN_RESPONSE) {
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

        if (state == ProxyLoginState.WAITING_LOGIN_RESPONSE) {
            buf.markReaderIndex()
            val size = buf.readUnsignedByte().toInt()
            if (buf.readableBytes() < size) {
                buf.resetReaderIndex()
                return
            }

            val payload = buf.readBytes(size)
            try {
                if (ctx.channel().attr(ProxyChannelAttributes.PACKET).get() is LoginPacket.LobbyLoginRequest) {
                    out.add(
                        LoginPacket.LobbyLoginResponse(
                            byte0 = payload.readUnsignedByte().toInt(),
                            rights = payload.readUnsignedByte().toInt(),
                            byte2 = payload.readUnsignedByte().toInt(),
                            byte3 = payload.readUnsignedByte().toInt(),
                            medium4 = payload.readUnsignedMedium(),
                            byte5 = payload.readUnsignedByte().toInt(),
                            byte6 = payload.readUnsignedByte().toInt(),
                            byte7 = payload.readUnsignedByte().toInt(),
                            long8 = payload.readLong(),
                            int9 = payload.readInt(),
                            byte10 = payload.readUnsignedByte().toInt(),
                            byte11 = payload.readUnsignedByte().toInt(),
                            int12 = payload.readInt(),
                            int13 = payload.readInt(),
                            short14 = payload.readUnsignedShort(),
                            short15 = payload.readUnsignedShort(),
                            short16 = payload.readUnsignedShort(),
                            ip = payload.readInt(),
                            byte17 = payload.readUnsignedByte().toInt(),
                            short18 = payload.readUnsignedShort(),
                            short19 = payload.readUnsignedShort(),
                            byte20 = payload.readUnsignedByte().toInt(),
                            username = payload.readNullCircumfixedString(),
                            byte22 = payload.readUnsignedByte().toInt(),
                            int23 = payload.readInt(),
                            short24 = payload.readUnsignedShort(),
                            defaultWorld = payload.readNullCircumfixedString(),
                            defaultWorldPort1 = payload.readUnsignedShort(),
                            defaultWorldPort2 = payload.readUnsignedShort()
                        )
                    )
                } else {
                    println(ByteBufUtil.prettyHexDump(payload))

                    val response = LoginPacket.GameLoginResponse(
                        byte0 = payload.readUnsignedByte().toInt(),
                        rights = payload.readUnsignedByte().toInt(),
                        byte2 = payload.readUnsignedByte().toInt(),
                        byte3 = payload.readUnsignedByte().toInt(),
                        byte4 = payload.readUnsignedByte().toInt(),
                        byte5 = payload.readUnsignedByte().toInt(), // <--> 6 byte
                        byte6 = payload.readUnsignedByte().toInt(),
                        playerIndex = payload.readUnsignedShort(),
                        byte8 = 0, //payload.readUnsignedByte().toInt(),
                        medium9 = payload.readMedium(),
                        isMember = payload.readUnsignedByte().toInt(),
                        username = payload.readNullCircumfixedString(),
                        short12 = payload.readUnsignedShort(),
                        int13 = payload.readInt()
                    )

                    ctx.channel().attr(ProxyChannelAttributes.PLAYER_INDEX).set(response.playerIndex)

                    out.add(response)
                }
            } finally {
                if (payload.isReadable)
                    logger.warn {
                        "Packet was still readable: ${
                            ctx.channel().attr(ProxyChannelAttributes.PACKET).get()::class.simpleName
                        }: ${payload.readableBytes()} bytes remaining"
                    }

                payload.release()
            }
            return
        }

        if (state == ProxyLoginState.WAITING_SERVERPERM_VARCS) {
            buf.markReaderIndex()
            val length = buf.readUnsignedShort()
            if (buf.readableBytes() < length) {
                buf.resetReaderIndex()
                return
            }

            val payload = buf.readBytes(length)
            try {
                val finished = payload.readUnsignedByte().toInt() == 1
                val values = Int2IntOpenHashMap()
                while (payload.isReadable) {
                    values[payload.readUnsignedShort()] = payload.readInt()
                }
                out.add(LoginPacket.ServerpermVarcChunk(finished, values))
            } finally {
                payload.release()
            }
            return
        }

        TODO("Not yet implemented")
    }
}