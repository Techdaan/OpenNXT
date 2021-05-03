package com.opennxt.tools.impl.cachedownloader

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.opennxt.net.js5.packet.Js5Packet
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import mu.KotlinLogging
import java.io.Closeable
import java.lang.Thread.sleep
import java.net.URL
import java.util.concurrent.Executors

class Js5ClientPool(
    private val numJs5Clients: Int = 3,
    private val numHttpClients: Int = 4,
    val ip: String,
    val port: Int
) : Closeable {
    private val logger = KotlinLogging.logger { }

    private val clients = arrayOfNulls<Js5Client>(numJs5Clients)

    private val httpExecutor = Executors.newFixedThreadPool(numHttpClients, ThreadFactoryBuilder()
        .setNameFormat("js5-http-executor-%d")
        .setUncaughtExceptionHandler { t, e -> logger.error { "Uncaught exception in js5 http download thread $t: $e" } }
        .build())
    private val bootstrap = Bootstrap()
    private val workerGroup = NioEventLoopGroup(8)
    private val credentials by lazy { Js5Credentials.download() }

    var closed = false

    init {
        bootstrap.group(workerGroup)
        bootstrap.channel(NioSocketChannel::class.java)
        bootstrap.handler(Js5ClientPipeline.Js5ClientChannelInitializer())
    }

    fun addRequest(priority: Boolean, index: Int, archive: Int): Js5RequestHandler.ArchiveRequest? {
        if (closed) throw IllegalStateException("Pool is closed!")

        val request = Js5RequestHandler.ArchiveRequest(index, archive, priority)

        return if (addRequest(request)) request else null
    }

    fun addRequestsFromIterator(
        it: MutableIterator<Js5RequestHandler.ArchiveRequest>,
        outJs5: MutableCollection<Js5RequestHandler.ArchiveRequest>
    ) {
        if (closed) throw IllegalStateException("Pool is closed!")

        while (it.hasNext()) {
            val request = it.next()

            if (request.index == 40)
                throw IllegalStateException("This shouldn't be reached (music requests shouldn't be in this list, but pendingHttp!)")
//            val isHttp = request.archive == 255
//            if (isHttp) {
//                httpExecutor.submit(
//                    Js5HttpRequest(
//                        URL(
//                            "http",
//                            ip,
//                            80,
//                            "/ms?m=0&a=${request.index}&k=${credentials.version}&g=${request.archive}&c=${request.crc}&v=${request.version}"
//                        ), request
//                    )
//                )
//                it.remove()
//                continue
//            }

            for (client in clients) {
                if (client == null)
                    continue

                synchronized(client.lock) {
                    val able = client.countAllowedRequests()
                    if (able > 0) {
                        val set = HashSet<Js5RequestHandler.ArchiveRequest>()
                        set += request
                        it.remove()

                        inner@
                        for (i in 1 until able) {
                            if (!it.hasNext())
                                break@inner
                            set += it.next()
                            it.remove()
                        }

                        outJs5.addAll(set)
                        client.addAllUnchecked(set)
                    }
                }
            }
        }
    }

    fun addRequest(request: Js5RequestHandler.ArchiveRequest): Boolean {
        if (closed) throw IllegalStateException("Pool is closed!")

        val isHttp = request.index == 40
        if (isHttp) {
            httpExecutor.submit(
                Js5HttpRequest(
                    URL(
                        "http",
                        ip,
                        80,
                        "/ms?m=0&a=${request.index}&k=${credentials.version}&g=${request.archive}&c=${request.crc}&v=${request.version}"
                    ), request
                )
            )
            return true
        } else {
            for (client in clients) {
                if (client == null) continue

                if (client.addRequest(request))
                    return true
            }

            return false
        }
    }

    fun getClient(): Js5Client =
        clients.first { it != null && it.state.canRead } ?: throw IllegalStateException("No clients available")

    fun openConnection(ip: String = this.ip, port: Int = this.port): Js5Client {
        if (closed) throw IllegalStateException("Pool is closed!")

        logger.info { "Opening connection to $ip:$port" }

        val channel = bootstrap.connect(ip, port).sync().channel()
        val client = channel.attr(Js5Client.ATTR_KEY).get()
        client.channel = channel

        logger.info { "Connected to $ip:$port! Sending handshake now." }

        channel.writeAndFlush(Js5Packet.Handshake(client.version, 1, client.token))

        return client
    }

    fun openConnections(ip: String = this.ip, port: Int = this.port, amount: Int = numJs5Clients) {
        if (closed) throw IllegalStateException("Pool is closed!")

        for (i in 0 until amount) {
            val existing = clients[i]

            if (existing != null)
                continue

            clients[i] = openConnection(ip, port)

            logger.info { "Waiting 1000ms after opening previous connection..." }
            sleep(1000)
        }
    }

    fun healthCheck() {
        if (closed) {
            return
        }

        var requiresOpening = false

        for (index in clients.indices) {
            val client = clients[index]
            if (client == null) {
                requiresOpening = true
                continue
            }

            if (System.currentTimeMillis() - client.lastRead > 30_000) {
                logger.info { "Read timeout on a js5 client - closing connection and re-opening later." }
                client.channel?.close()?.sync()
                client.markAsCrashed()
                client.state = Js5ClientState.DISCONNECTED
                clients[index] = null
                requiresOpening = true
                continue
            }

            if (client.channel?.isOpen == false || !client.state.canRead) {
                logger.info { "Channel is not open, or client is in a state where we can't read, closing and re-opening later." }
                client.channel?.close()?.sync()
                client.markAsCrashed()
                client.state = Js5ClientState.DISCONNECTED
                clients[index] = null
                requiresOpening = true
                continue
            }
        }

        if (requiresOpening) {
            logger.info { "Re-opening dead/closed connections." }
            openConnections(ip, port)
        }
    }

    override fun close() {
        closed = true

        for (index in clients.indices) {
            val client = clients[index]

            if (client != null) {
                client.channel?.close()?.sync()
                client.state = Js5ClientState.DISCONNECTED
                clients[index] = null
                logger.info { "Closing client (reason = pool close)" }
            }
        }
    }
}