package com.opennxt.net

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import mu.KotlinLogging

class RSChannelInitializer: ChannelInitializer<SocketChannel>() {
    val logger = KotlinLogging.logger {  }

    override fun initChannel(ch: SocketChannel) {
        logger.info { "TODO : Accept inbound connection from ${ch.remoteAddress()} to port ${ch.localAddress().port}" }
    }
}