package com.opennxt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.opennxt.config.RsaConfig
import com.opennxt.config.ServerConfig
import com.opennxt.config.TomlConfig
import com.opennxt.filesystem.ChecksumTable
import com.opennxt.filesystem.Container
import com.opennxt.filesystem.Filesystem
import com.opennxt.filesystem.prefetches.PrefetchTable
import com.opennxt.filesystem.sqlite.SqliteFilesystem
import com.opennxt.login.LoginThread
import com.opennxt.net.RSChannelInitializer
import com.opennxt.net.http.HttpServer
import com.opennxt.net.proxy.ProxyConnectionFactory
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import mu.KotlinLogging
import java.io.FileNotFoundException
import kotlin.system.exitProcess

object OpenNXT : CliktCommand(name = "run-server", help = "Launches the OpenNXT server)") {
    val skipHttpFileVerification by option(help = "Skips file verification when http server starts").flag(default = false)
    val enableProxySupport by option(help = "Enables proxy support. Disable this on live or when you won't use it.").flag(
        default = false
    )

    private val logger = KotlinLogging.logger {}

    lateinit var config: ServerConfig
    lateinit var rsaConfig: RsaConfig

    lateinit var http: HttpServer
    lateinit var prefetches: PrefetchTable
    lateinit var checksumTable: ByteArray
    lateinit var httpChecksumTable: ByteArray

    lateinit var filesystem: Filesystem
    lateinit var proxyConnectionFactory: ProxyConnectionFactory

    private val bootstrap = ServerBootstrap()

    private fun loadConfigurations() {
        logger.info { "Loading configuration files from ${Constants.CONFIG_PATH}" }
        config = TomlConfig.load(Constants.CONFIG_PATH.resolve("server.toml"))
        rsaConfig = try {
            TomlConfig.load(RsaConfig.DEFAULT_PATH, mustExist = true)
        } catch (e: FileNotFoundException) {
            logger.info { "Could not find RSA config: $e. Please run `run-tool rsa-key-generator`" }
            exitProcess(1)
        }
    }

    override fun run() {
        logger.info { "Starting OpenNXT" }
        loadConfigurations()

        if (enableProxySupport) {
            logger.warn { "---------------- WARNING ----------------" }
            logger.warn { " You are running in proxy-enabled mode." }
            logger.warn { " Disable this in production environments" }
            logger.warn { " or when you are not going to use this." }
            logger.warn { "" }
            logger.warn { " Remove flag '--enable-proxy-support'." }
            logger.warn { " to disable." }
            logger.warn { "---------------- WARNING ----------------" }

            logger.info { "Setting up proxy connection factory" }
            proxyConnectionFactory = ProxyConnectionFactory()
        }

        logger.info { "Setting up HTTP server" }
        http = HttpServer(config)
        http.init(skipHttpFileVerification)
        http.bind()

        logger.info { "Opening filesystem from ${Constants.CACHE_PATH}" }
        filesystem = SqliteFilesystem(Constants.CACHE_PATH)

        logger.info { "Generating prefetch table" }
        prefetches = PrefetchTable.of(filesystem)

        logger.info { "Generating & encoding checksum tables" }
        checksumTable = Container.wrap(
            ChecksumTable.create(filesystem, false)
                .encode(rsaConfig.js5.modulus, rsaConfig.js5.exponent)
        ).array()
        httpChecksumTable = Container.wrap(
            ChecksumTable.create(filesystem, true)
                .encode(rsaConfig.js5.modulus, rsaConfig.js5.exponent)
        ).array()

        logger.info { "Starting js5 thread" }
        Js5Thread.start()

        logger.info { "Starting login thread" }
        LoginThread.start()

        logger.info { "Starting network" }
        bootstrap.group(NioEventLoopGroup())
            .channel(NioServerSocketChannel::class.java)
            .childHandler(RSChannelInitializer())
            .childOption(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)

        logger.info { "Binding game server to 0.0.0.0:${config.ports.game}" }
        val result = bootstrap.bind("0.0.0.0", config.ports.game).sync()
        if (!result.isSuccess) {
            logger.error(result.cause()) { "Failed to bind to 0.0.0.0:${config.ports.game}" }
            exitProcess(1)
        }
        logger.info { "Game server bound to 0.0.0.0:${config.ports.game}" }
    }
}