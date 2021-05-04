package com.opennxt.login

import com.opennxt.OpenNXT
import com.opennxt.net.RSChannelAttributes
import com.opennxt.net.login.LoginPacket
import com.opennxt.net.proxy.ProxyChannelAttributes
import com.opennxt.net.proxy.ProxyConnectionFactory
import io.netty.channel.Channel
import mu.KotlinLogging
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

// TODO This should probably be re-done entirely.
object LoginThread : Thread("login-thread") {
    private val logger = KotlinLogging.logger { }

    val queue = LinkedBlockingQueue<LoginContext>()
    val running = AtomicBoolean(true)

    override fun run() {
        while (running.get()) {
            try {
                val next = queue.take()

                process(next)
            } catch (e: Exception) {
                logger.error(e) { "Uncaught exception occurred handling login request" }
            }
        }
    }

    private fun process(context: LoginContext) {
        if (OpenNXT.enableProxySupport) {
            OpenNXT.proxyConnectionFactory.createLogin(context.packet) { channel, result ->
                if (channel != null) {
                    context.channel.attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).set(channel)
                    channel.attr(RSChannelAttributes.PASSTHROUGH_CHANNEL).set(context.channel)
                }

                context.callback(result)
            }
        } else {
            context.callback(LoginResult.SUCCESS)
        }
    }

    fun login(packet: LoginPacket, channel: Channel, callback: (LoginResult) -> Unit) {
        when (packet) {
            is LoginPacket.LobbyLoginRequest -> {
                queue.add(
                    LoginContext(
                        packet,
                        callback,
                        packet.build,
                        packet.username,
                        packet.password,
                        channel = channel
                    )
                )
            }
            else -> throw IllegalArgumentException("expected LobbyLoginRequest or GameLoginRequest, got $packet")
        }
    }
}