package com.opennxt.net.proxy

import com.opennxt.net.login.LoginEncoder
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import mu.KotlinLogging

class ProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    private val logger = KotlinLogging.logger { }

    override fun initChannel(ch: SocketChannel) {
        ch.attr(ProxyChannelAttributes.LOGIN_STATE).set(ProxyLoginState.HANDSHAKE)

        ch.pipeline()
            .addLast("login-decoder", LoginClientDecoder())
            .addLast("login-handler", LoginClientHandler())
            .addLast("login-encoder", LoginEncoder())
    }
}