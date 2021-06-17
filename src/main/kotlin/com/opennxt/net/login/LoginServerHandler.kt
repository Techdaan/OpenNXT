package com.opennxt.net.login

import com.opennxt.OpenNXT
import com.opennxt.login.LoginThread
import com.opennxt.model.lobby.LobbyPlayer
import com.opennxt.model.lobby.TODORefactorThisClass
import com.opennxt.net.GenericResponse
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.game.pipeline.DynamicPacketHandler
import com.opennxt.net.game.pipeline.GamePacketEncoder
import com.opennxt.net.game.pipeline.GamePacketFraming
import com.opennxt.util.ISAACCipher
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import mu.KotlinLogging

class LoginServerHandler : SimpleChannelInboundHandler<LoginPacket>() {
    private val logger = KotlinLogging.logger { }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginPacket) {
        if (msg is LoginPacket.LobbyLoginRequest || msg is LoginPacket.GameLoginRequest) {
            val header = if (msg is LoginPacket.LobbyLoginRequest) msg.header
            else (msg as LoginPacket.GameLoginRequest).header

            ctx.channel().attr(RSChannelAttributes.INCOMING_ISAAC).set(ISAACCipher(header.seeds))
            ctx.channel().attr(RSChannelAttributes.OUTGOING_ISAAC)
                .set(ISAACCipher(header.seeds.map { it + 50 }.toIntArray()))

            LoginThread.login(msg, ctx.channel()) {
                val future = ctx.channel().writeAndFlush(Unpooled.buffer(1).writeByte(it.result.code.id))
                if (it.result.code != GenericResponse.SUCCESSFUL) {
                    future.addListener(ChannelFutureListener.CLOSE)
                    return@login
                }

                if (ctx.channel().attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).get() != null) {
                    logger.info { "Login was OK for proxy connection [client->open nxt], leaving channel management to proxy..." }
                } else {
                    if (ctx.channel().attr(RSChannelAttributes.LOGIN_TYPE).get() == LoginType.GAME){

                        val map = Int2IntOpenHashMap()
                        TODORefactorThisClass.populateServerpermVarcs(map)
                        val response = LoginPacket.ServerpermVarcChunk(true, emptyMap())
                        ctx.channel().writeAndFlush(response)

                        logger.info { "Sending serverperm varcs" }

                        return@login
                    }

                    val response = LoginPacket.LobbyLoginResponse(
                        byte0 = 0,
                        rights = 2,
                        byte2 = 0,
                        byte3 = 0,
                        medium4 = 0,
                        byte5 = 0,
                        byte6 = 0,
                        byte7 = 0,
                        long8 = 0,
                        int9 = 0,
                        byte10 = 0,
                        byte11 = 0,
                        int12 = 0,
                        int13 = 0,
                        short14 = 0,
                        short15 = 0,
                        short16 = 0,
                        ip = 213076433,
                        byte17 = 0,
                        short18 = 0,
                        short19 = 0,
                        byte20 = 0,
                        username = it.username,
                        byte22 = 0,
                        int23 = 0,
                        short24 = 0,
                        defaultWorld = OpenNXT.config.hostname,
                        defaultWorldPort1 = 43594,
                        defaultWorldPort2 = 43594,
                    )

                    ctx.channel().writeAndFlush(response).addListener { future ->
                        if (!future.isSuccess) {
                            logger.error(future.cause()) { "Failed to write login response" }
                            ctx.channel().close()
                            return@addListener
                        }

                        ctx.channel().pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
                        ctx.channel().pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
                        ctx.channel().pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())

                        val player = LobbyPlayer(ctx.channel().attr(RSChannelAttributes.CONNECTED_CLIENT).get(), it.username)

                        OpenNXT.lobby.addPlayer(player)
                    }

                    logger.info { "Login on [SERVER] is completed. Should add this to a map somewhere to handle" }
                }
            }
        } else throw IllegalStateException("idk how to handle $msg!")
    }
}