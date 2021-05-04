package com.opennxt.net.game.protocol

import com.opennxt.OpenNXT
import com.opennxt.config.TomlConfig
import com.opennxt.net.game.PacketRegistry
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

class ProtocolInformation(val path: Path) {
    private val logger = KotlinLogging.logger {  }
    lateinit var clientProtSizes: Opcode2SizeConfig
    lateinit var serverProtSizes: Opcode2SizeConfig
    lateinit var clientProtNames: Name2OpcodeConfig
    lateinit var serverProtNames: Name2OpcodeConfig

    fun load() {
        logger.info { "Loading protocol information from $path" }

        try {
            clientProtSizes = TomlConfig.load(path.resolve("clientProtSizes.toml"), mustExist = true)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error { "Protocol information not found for build ${OpenNXT.config.build}." }
            logger.error { " Looked in: ${path.resolve("clientProtSizes.toml")}" }
            logger.error { " Please look check out the following wiki page for help: <TO-DO>" }
            exitProcess(1)
        }

        try {
            serverProtSizes = TomlConfig.load(path.resolve("serverProtSizes.toml"), mustExist = true)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error { "Protocol information not found for build ${OpenNXT.config.build}." }
            logger.error { " Looked in: ${path.resolve("serverProtSizes.toml")}" }
            logger.error { " Please look check out the following wiki page for help: <TO-DO>" }
            exitProcess(1)
        }

        try {
            clientProtNames = TomlConfig.load(path.resolve("clientProtNames.toml"), mustExist = true)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error { "Protocol information not found for build ${OpenNXT.config.build}." }
            logger.error { " Looked in: ${path.resolve("clientProtNames.toml")}" }
            logger.error { " Please look check out the following wiki page for help: <TO-DO>" }
            exitProcess(1)
        }

        try {
            serverProtNames = TomlConfig.load(path.resolve("serverProtNames.toml"), mustExist = true)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error { "Protocol information not found for build ${OpenNXT.config.build}." }
            logger.error { " Looked in: ${path.resolve("serverProtNames.toml")}" }
            logger.error { " Please look check out the following wiki page for help: <TO-DO>" }
            exitProcess(1)
        }

        refreshPacketCodecs()
    }

    fun refreshPacketCodecs() {
        logger.info { "Refreshing packet codecs" }

        if (!Files.exists(path.resolve("clientProt")))
            Files.createDirectories(path.resolve("clientProt"))

        if (!Files.exists(path.resolve("serverProt")))
            Files.createDirectories(path.resolve("serverProt"))

        PacketRegistry.reload()
    }
}