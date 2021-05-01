package com.opennxt.tools.impl

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.opennxt.Constants
import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.ClientConfig
import com.opennxt.tools.Tool
import lzma.sdk.lzma.Decoder
import lzma.streams.LzmaInputStream
import java.io.ByteArrayInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class ClientDownloader : Tool("client-downloader", "Downloads clients from RS3") {

    val configURL by option(help = "The base URL where RuneScape serves game configs from. Must include jav_config.ws")
        .default("https://world5.runescape.com/jav_config.ws")

    override fun runTool() {
        logger.info { "Downloading all binary type clients..." }

        var version = -1
        lateinit var path: Path
        BinaryType.values().forEach { type ->
            logger.info { "Downloading config ${type.name} (id=${type.id})" }
            val config = ClientConfig.download(configURL, type)
            if (version == -1) {
                version = config.getValue("server_version")!!.toInt()
                path = Constants.CLIENTS_PATH.resolve(version.toString())
                logger.info { "Current game version: $version" }
                logger.info { "Saving clients in $path" }
                if (!Files.exists(path)) Files.createDirectories(path)
            }

            val typePath = path.resolve(type.name.toLowerCase()).resolve("original")
            if (!Files.exists(typePath)) Files.createDirectories(typePath)

            ClientConfig.save(config, typePath.resolve("jav_config.ws"))

            val codebase = config.getValue("codebase")
            config.getFiles().forEach { file ->
                val url = URL("${codebase}client?binaryType=${type.id}&fileName=${file.name}&crc=${file.crc}")
                logger.info { "Downloading file ${file.name} from $url" }

                val content = url.readBytes()
                logger.info { " Decompressing ${file.name} (compressed size ${content.size})" }

                val saveTo = typePath.resolve(file.name)
                val decompressed = decompressLZMA(content)
                logger.info { " Decompressed ${file.name} (decompressed size ${decompressed.size}). Writing to $saveTo" }

                Files.write(saveTo, decompressed)
            }
        }

        logger.info { "Done downloading clients!" }
    }

    private fun decompressLZMA(raw: ByteArray): ByteArray =
        LzmaInputStream(ByteArrayInputStream(raw), Decoder()).use { it.readBytes() }
}