package com.opennxt.net.login

import com.opennxt.config.RsaConfig
import com.opennxt.ext.decipherXtea
import com.opennxt.ext.readBuild
import com.opennxt.ext.readString
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.login.LoginRSAHeader.Companion.readLoginHeader
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mu.KotlinLogging
import kotlin.system.exitProcess

class LoginServerDecoder(val rsaPair: RsaConfig.RsaKeyPair) : ByteToMessageDecoder() {
    private val logger = KotlinLogging.logger {  }

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        buf.markReaderIndex()

        val id = buf.readUnsignedByte().toInt()
        val type = LoginType.fromId(id)
        if (type == null) {
            logger.warn("Client from ${ctx.channel().remoteAddress()} attempted to login with unknown id: $id")
            buf.skipBytes(buf.readableBytes())
            ctx.close()
            return
        }

        // TODO Move this to another decoder or something, this should *probably* not be here.
        if (type == LoginType.GAMELOGIN_CONTINUE) {
            if (ctx.channel().attr(RSChannelAttributes.LOGIN_TYPE).get() == null) {
                logger.error { "GAMELOGIN_CONTINUE sent from client that is not in login state" }
                buf.skipBytes(buf.readableBytes())
                ctx.channel().close()
                return
            }

            logger.info { "TODO: Send game login response" }

            // TODO Send login response here? Why are we doing this here...
            return
        }

        if (buf.readableBytes() < 2){
            buf.resetReaderIndex()
            return
        }

        val length = buf.readUnsignedShort()
        if (buf.readableBytes() < length) {
            buf.resetReaderIndex()
            return
        }

        ctx.channel().attr(RSChannelAttributes.LOGIN_TYPE).set(type)

        val payload = buf.readBytes(length)
        try {
            val build = payload.readBuild()
            val header = payload.readLoginHeader(type,rsaPair.exponent, rsaPair.modulus)

            if (header !is LoginRSAHeader.Fresh && type != LoginType.LOBBY) {
                logger.info { "got reconnecting block in lobby? what?" } // literally impossible but ok.
                ctx.channel()
                    .writeAndFlush(LoginPacket.LoginResponse(GenericResponse.MALFORMED_PACKET))
                    .addListener(ChannelFutureListener.CLOSE)
                return
            }

            payload.decipherXtea(header.seeds)

            payload.markReaderIndex()
            val original = ByteArray(payload.readableBytes())
            payload.readBytes(original)
            payload.resetReaderIndex()

            payload.skipBytes(1) // TODO This has to do with name being encoded as long, should prolly check it
            val name = payload.readString()

            if (header.uniqueId != ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).get()) {
                logger.error { "Unique id mismatch - possible replay attack?" }
                ctx.channel()
                    .writeAndFlush(LoginPacket.LoginResponse(GenericResponse.MALFORMED_PACKET))
                    .addListener(ChannelFutureListener.CLOSE)
                return
            }

            when (type) {
                LoginType.LOBBY -> {
                    header as LoginRSAHeader.Fresh

                    logger.info { "Attempted lobby login: $name, *****" }
                    out.add(LoginPacket.LobbyLoginRequest(build, header, name, header.password, Unpooled.wrappedBuffer(original)))
                }
                LoginType.GAME -> {
                    if (header !is LoginRSAHeader.Fresh) {
                        logger.warn { "Client attempted to reconnect, this isn't supported yet!" }
                        ctx.channel()
                            .writeAndFlush(LoginPacket.LoginResponse(GenericResponse.LOGINSERVER_REJECTED))
                            .addListener(ChannelFutureListener.CLOSE)
                        return
                    }

                    logger.info { "Attempted game login: $name, *****" }
                    out.add(LoginPacket.GameLoginRequest(build, header, name, header.password, Unpooled.wrappedBuffer(original)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ctx.channel()
                .writeAndFlush(LoginPacket.LoginResponse(GenericResponse.MALFORMED_PACKET))
                .addListener(ChannelFutureListener.CLOSE)
        } finally {
            payload.release()
        }
    }
}