package com.opennxt.tools.impl

import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.opennxt.Constants
import com.opennxt.config.RsaConfig
import com.opennxt.config.TomlConfig
import com.opennxt.tools.Tool
import java.io.FileNotFoundException
import java.nio.file.Files
import kotlin.system.exitProcess

class ClientPatcher :
    Tool("client-patcher", "Patches all clients and configs files. Uses most recent revision by default") {
    private val version by option(help = "The version of the client to patch")
        .int()
        .defaultLazy {
            val path = Constants.CLIENTS_PATH
            if (!Files.exists(path)) return@defaultLazy -1

            var version = -1
            Files.list(path).forEach {
                try {
                    val thisVersion = it.fileName.toString().toInt()
                    if (version < thisVersion) version = thisVersion
                } catch (e: NumberFormatException) {
                }
            }

            version
        }

    override fun runTool() {
        logger.info { "Patching clients for version $version" }

        val path = Constants.CLIENTS_PATH.resolve(version.toString())
        if (!Files.exists(path)) throw FileNotFoundException("$path: do clients with version $version exist? did you run `run-tool client-downloader` yet?")

        logger.info { "Patching clients in $path" }
        val rsaConfig = try {
            TomlConfig.load<RsaConfig>(RsaConfig.DEFAULT_PATH, mustExist = true)
        } catch (e: FileNotFoundException) {
            logger.info { "Could not find RSA config: $e. Please run `run-tool rsa-key-generator`" }
            exitProcess(1)
        }

        logger.info { "RSA config: $rsaConfig" }
    }
}