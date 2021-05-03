package com.opennxt.login

import com.opennxt.net.login.LoginPacket
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
        context.callback(LoginResult.LOGGED_IN)
    }

    fun login(packet: LoginPacket, callback: (LoginResult) -> Unit) {
        when (packet) {
            is LoginPacket.LobbyLoginRequest -> {
                queue.add(
                    LoginContext(
                        packet,
                        callback,
                        packet.build,
                        packet.username,
                        packet.password,
                    )
                )
            }
            else -> throw IllegalArgumentException("expected LobbyLoginRequest or GameLoginRequest, got $packet")
        }
    }
}