package com.opennxt.net.proxy

import com.opennxt.OpenNXT
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

    private fun createGameLoginPacket(
        original: LoginPacket.GameLoginRequest,
        username: String,
        password: String,
        uniqueId: Long
    ): LoginPacket.GameLoginRequest {
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

        return LoginPacket.GameLoginRequest(original.build, header, username, password, newBody)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        try {
            val state = ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).get()
            when (msg) {
                is LoginPacket.LoginResponse -> {
                    if (state == ProxyLoginState.HANDSHAKE) {
                        if (msg.code != GenericResponse.SUCCESSFUL_CONNECTION) {
                            ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(
                                ctx.channel(),
                                LoginResult.reverse(msg.code)
                            )
                            ctx.channel().close()
                            return
                        }

                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.UNIQUE_ID)
                    } else if (state == ProxyLoginState.LOGIN_RESPONSE) {
                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_HANDLER).get()(
                            ctx.channel(),
                            LoginResult.reverse(msg.code)
                        )

                        if (msg.code == GenericResponse.SUCCESSFUL) {
                            val originalPacket = ctx.channel().attr(ProxyChannelAttributes.PACKET).get()
                            if (originalPacket is LoginPacket.LobbyLoginRequest) {
                                ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE)
                                    .set(ProxyLoginState.WAITING_LOGIN_RESPONSE)
                            } else {
                                ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE)
                                    .set(ProxyLoginState.WAITING_SERVERPERM_VARCS)
                            }
                        } else {
                            ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.FINISHED)
                        }
                    } else if (state == ProxyLoginState.WAITING_WORLDLOGIN_RESPONSE){
                        logger.info { "Got worldlogin response: ${msg.code}" }
                        if (msg.code == GenericResponse.SUCCESSFUL) {
                            ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE)
                                .set(ProxyLoginState.WAITING_LOGIN_RESPONSE)
                        } else {
                            ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.FINISHED)
                        }
                    }
                }

                is LoginPacket.SendUniqueId -> {
                    ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).set(msg.id)
                    ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.LOGIN_RESPONSE)

                    val originalPacket = ctx.channel().attr(ProxyChannelAttributes.PACKET).get()
                    if (originalPacket is LoginPacket.LobbyLoginRequest) {
                        val newPacket = createLobbyLoginPacket(
                            originalPacket,
                            ctx.channel().attr(ProxyChannelAttributes.USERNAME).get(),
                            ctx.channel().attr(ProxyChannelAttributes.PASSWORD).get(),
                            ctx.channel().attr(RSChannelAttributes.LOGIN_UNIQUE_ID).get(),
                        )

                        ctx.channel().attr(RSChannelAttributes.OUTGOING_ISAAC)
                            .set(ISAACCipher(newPacket.header.seeds.clone()))
                        ctx.channel().attr(RSChannelAttributes.INCOMING_ISAAC)
                            .set(ISAACCipher(IntArray(4) { newPacket.header.seeds[it] + 50 }))
                        ctx.channel().writeAndFlush(newPacket)
                    } else {
                        val newPacket = createGameLoginPacket(
                            originalPacket as LoginPacket.GameLoginRequest,
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
                }

                is LoginPacket.LobbyLoginResponse -> {
                    logger.info { "proxy client -> game protocol" }
                    ctx.channel().pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())
                    ctx.channel().pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                    ctx.channel().pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                    ctx.channel().attr(ProxyChannelAttributes.PROXY_CLIENT).get().ready = true

                    val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get() ?: return

                    // we want to be admin at all times >:D
                    passthrough.writeAndFlush(
                        msg.copy(rights = 2, defaultWorld = OpenNXT.config.hostname)
                    ).addListener {
                        if (!it.isSuccess) {
                            logger.error(it.cause()) { "???" }
                        }

                        logger.info { "game client -> game protocol" }
                        passthrough.pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())
                        passthrough.pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                        passthrough.pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                        passthrough.attr(ProxyChannelAttributes.PROXY_CLIENT).get().ready = true
                    }
                }

                is LoginPacket.GameLoginResponse -> {
                    logger.info { "proxy client -> got response: $msg; switching to world" }

                    ctx.channel().pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())
                    ctx.channel().pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                    ctx.channel().pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                    ctx.channel().attr(ProxyChannelAttributes.PROXY_CLIENT).get().ready = true

                    val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get() ?: return

                    // we want to be admin at all times >:D
                    passthrough.write(Unpooled.buffer(1).writeByte(2))
                    passthrough.writeAndFlush(msg.copy(rights = 2)).addListener {
                        if (!it.isSuccess) {
                            logger.error(it.cause()) { "???" }
                        }

                        logger.info { "game client -> game protocol" }
                        passthrough.pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())
                        passthrough.pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                        passthrough.pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                        passthrough.attr(ProxyChannelAttributes.PROXY_CLIENT).get().ready = true
                    }

//                    ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.WAITING_SERVERPERM_VARCS)
//
//                    val passthrough = ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get() ?: return
//
//                    // we want to be admin at all times >:D
//                    passthrough.writeAndFlush(msg.copy(rights = 2))
                }

                is LoginPacket.ServerpermVarcChunk -> {
                    logger.info { "Got serverperm varcs, count=${msg.varcs.size}, finished=${msg.finished}" }

                    ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get()?.writeAndFlush(msg)

                    if (msg.finished) {
                        logger.info { "Setting state to WAITING_WORLDLOGIN_RESPONSE, sending 26" }

                        ctx.channel().attr(ProxyChannelAttributes.LOGIN_STATE)
                            .set(ProxyLoginState.WAITING_WORLDLOGIN_RESPONSE)

                        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(26))
                    }
                }

                else -> {
                    logger.info { "TODO : Handle message $msg" }
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(HandshakeType.LOGIN.id))
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error(cause) { "SOME EXCEPTION OCCURRED!" }
    }
}