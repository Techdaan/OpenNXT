package com.opennxt.net.proxy

import com.opennxt.login.LoginResult
import com.opennxt.net.ConnectedClient
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.Side
import com.opennxt.net.login.LoginPacket
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import mu.KotlinLogging

class ProxyConnectionFactory {
    private val logger = KotlinLogging.logger { }
    private val bootstrap = Bootstrap()
    private val workerGroup = NioEventLoopGroup(8)

    init {
        bootstrap.group(workerGroup)
        bootstrap.channel(NioSocketChannel::class.java)
        bootstrap.handler(ProxyChannelInitializer())
    }

    fun createLogin(packet: LoginPacket, callback: (Channel?, LoginResult) -> Unit) {
        if (packet !is LoginPacket.LobbyLoginRequest && packet !is LoginPacket.GameLoginRequest)
            throw NullPointerException("Login should be LobbyLoginRequest or GameLoginRequest!")

        val host = if (packet is LoginPacket.LobbyLoginRequest) "lobby18a.runescape.com" else "world3.runescape.com"

        bootstrap.connect(host, 43594).addListener(ChannelFutureListener { listener ->
            if (!listener.isSuccess) {
                logger.warn(listener.cause()) { "Failed to connect to server" }
                callback(null, LoginResult.BANNED)
                return@ChannelFutureListener
            }

            val ch = listener.channel()
            ch.attr(RSChannelAttributes.SIDE).set(Side.SERVER)
            ch.attr(RSChannelAttributes.CONNECTED_CLIENT).set(ConnectedClient(Side.SERVER, ch))
            ch.attr(ProxyChannelAttributes.LOGIN_HANDLER).set(callback)
            ch.attr(ProxyChannelAttributes.USERNAME)
                .set(if (packet is LoginPacket.LobbyLoginRequest) packet.username else if (packet is LoginPacket.GameLoginRequest) packet.username else throw IllegalStateException())
            ch.attr(ProxyChannelAttributes.PASSWORD)
                .set(if (packet is LoginPacket.LobbyLoginRequest) packet.password else if (packet is LoginPacket.GameLoginRequest) packet.password else throw IllegalStateException())
            ch.attr(ProxyChannelAttributes.PACKET).set(packet)
        })
    }

}