package com.opennxt.net.proxy

import com.opennxt.ext.encipherXtea
import com.opennxt.ext.readString
import com.opennxt.ext.writeString
import com.opennxt.login.LoginResult
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.game.pipeline.DynamicPacketHandler
import com.opennxt.net.game.pipeline.GamePacketEncoder
import com.opennxt.net.game.pipeline.GamePacketFraming
import com.opennxt.net.handshake.HandshakeType
import com.opennxt.net.login.LoginPacket
import com.opennxt.net.login.LoginRSAHeader
import com.opennxt.util.ISAACCipher
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
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
        newBody.encipherXtea(seeds.clone())

        return LoginPacket.LobbyLoginRequest(original.build, header, username, password, newBody)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
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
                    if (msg.code == GenericResponse.SUCCESSFUL) {
                        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(msg.code.id))
                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE)
                            .set(ProxyLoginState.WAITING_SERVER_RESPOSNE)
                    } else {
                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(LoginResult.reverse(msg.code))
                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.FINISHED)
                        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(msg.code.id))
                            .addListener(ChannelFutureListener.CLOSE)
                    }
                } else {
                    TODO("Unsure what to do here.")
                }
            }

            is LoginPacket.SendUniqueId -> {
                ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).set(msg.id)
                ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.LOGIN_RESPONSE)

                val newPacket = createLobbyLoginPacket(
                    ctx.channel().attr(ProxyChannelAttributes.PACKET).get() as LoginPacket.LobbyLoginRequest,
                    ctx.channel().attr(ProxyChannelAttributes.USERNAME).get(),
                    ctx.channel().attr(ProxyChannelAttributes.PASSWORD).get(),
                    ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).get(),
                )

                ctx.channel().attr(RSChannelAttributes.OUTGOING_ISAAC)
                    .set(ISAACCipher(newPacket.header.seeds.clone()))
                ctx.channel().attr(RSChannelAttributes.INCOMING_ISAAC)
                    .set(ISAACCipher(IntArray(4) { newPacket.header.seeds[it] + 50 }))

                ctx.channel().writeAndFlush(newPacket)
            }

            is LoginPacket.LobbyLoginResponse -> {
                ctx.channel().pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                ctx.channel().pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                ctx.channel().pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())

                ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(LoginResult.SUCCESS)
            }

            else -> TODO("todo handle $msg")
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(HandshakeType.LOGIN.id))
    }
}