package com.opennxt.net.proxy

import com.opennxt.ext.encipherXtea
import com.opennxt.ext.readString
import com.opennxt.ext.writeString
import com.opennxt.login.LoginResult
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.handshake.HandshakeType
import com.opennxt.net.login.LoginPacket
import com.opennxt.net.login.LoginRSAHeader
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import mu.KotlinLogging
import java.util.concurrent.ThreadLocalRandom

class LoginClientHandler : SimpleChannelInboundHandler<LoginPacket>() {
    private val logger = KotlinLogging.logger { }

    private fun createLobbyLoginPacket(
        original: LoginPacket.LobbyLoginRequest,
        username: String,
        password: String,
        uniqueId: Long
    ): LoginPacket.LobbyLoginRequest {
        val seeds = intArrayOf(
            ThreadLocalRandom.current().nextInt(),
            ThreadLocalRandom.current().nextInt(),
            ThreadLocalRandom.current().nextInt(),
            ThreadLocalRandom.current().nextInt()
        )

        val oldHeader = original.header as LoginRSAHeader.Fresh
        val header = LoginRSAHeader.Fresh(
            seeds,
            uniqueId,
            oldHeader.weirdThingId,
            oldHeader.weirdThingValue,
            oldHeader.thatBoolean,
            password,
            oldHeader.someLong,
            oldHeader.randClient
        )

        val oldBody = original.remaining

        val newBody = Unpooled.buffer()
        newBody.writeByte(oldBody.readUnsignedByte().toInt())

        oldBody.readString() // move to beyond old name
        newBody.writeString(username)

        newBody.writeBytes(oldBody)

        newBody.readerIndex(0)
        newBody.encipherXtea(seeds)

        return LoginPacket.LobbyLoginRequest(original.build, header, username, password, newBody)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        logger.info { "Proxy recv $msg" }

        val state = ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).get()
        when (msg) {
            is LoginPacket.LoginResponse -> {
                if (state == ProxyLoginState.HANDSHAKE) {
                    if (msg.code != GenericResponse.SUCCESSFUL_CONNECTION) {
                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(LoginResult.reverse(msg.code))
                        ctx.channel().close()
                        return
                    }

                    ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.UNIQUE_ID)
                } else if (state == ProxyLoginState.LOGIN_RESPONSE) {
                    ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(LoginResult.reverse(msg.code))
                    logger.info { "Final login response: ${msg.code}" }
                    ctx.channel().close()
                } else {
                    TODO("Unsure what to do here.")
                }
            }

            is LoginPacket.SendUniqueId -> {
                ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).set(msg.id)
                ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.LOGIN_RESPONSE)
                ctx.channel().writeAndFlush(
                    createLobbyLoginPacket(
                        ctx.channel().attr(ProxyChannelAttributes.PACKET).get() as LoginPacket.LobbyLoginRequest,
                        ctx.channel().attr(ProxyChannelAttributes.USERNAME).get(),
                        ctx.channel().attr(ProxyChannelAttributes.PASSWORD).get(),
                        ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).get(),
                    )
                )
            }

            else -> TODO("todo handle $msg")
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(HandshakeType.LOGIN.id))
    }
}