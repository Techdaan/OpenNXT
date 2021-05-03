package com.opennxt

import com.github.ajalt.clikt.core.CliktCommand
import com.opennxt.config.RsaConfig
import com.opennxt.config.ServerConfig
import com.opennxt.config.TomlConfig
import com.opennxt.net.http.HttpServer
import mu.KotlinLogging
import java.io.FileNotFoundException
import kotlin.system.exitProcess

object OpenNXT : CliktCommand(name = "run-server", help = "Launches the OpenNXT server)") {
    private val logger = KotlinLogging.logger {}

    lateinit var config: ServerConfig
    lateinit var rsaConfig: RsaConfig

    lateinit var http: HttpServer

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

        logger.info { "Setting up HTTP server" }
        http = HttpServer(config)
        http.init()
        http.bind()

    }
}