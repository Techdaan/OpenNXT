package com.opennxt.net

import com.opennxt.net.handshake.HandshakeDecoder
import com.opennxt.net.handshake.HandshakeHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import mu.KotlinLogging

class RSChannelInitializer: ChannelInitializer<SocketChannel>() {
    val logger = KotlinLogging.logger {  }

    override fun initChannel(ch: SocketChannel) {
        logger.info { "TODO : Accept inbound connection from ${ch.remoteAddress()} to port ${ch.localAddress().port}" }

        ch.pipeline()
            .addLast("handshake-decoder", HandshakeDecoder())
            .addLast("handshake-handler", HandshakeHandler())
    }
}