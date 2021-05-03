package com.opennxt.model.files

import com.opennxt.Constants
import com.opennxt.OpenNXT
import com.opennxt.config.RsaConfig
import com.opennxt.tools.impl.ClientPatcher
import lzma.sdk.lzma.Decoder
import lzma.streams.LzmaInputStream
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.zip.CRC32

object FileChecker {
    private val logger = KotlinLogging.logger { }

    fun latestBuild(): Int {
        var build = -1
        Files.list(Constants.CLIENTS_PATH).forEach {
            try {
                val thisBuild = it.fileName.toString().toInt()
                if (build < thisBuild) build = thisBuild
            } catch (e: NumberFormatException) {
            }
        }
        return build
    }

    fun getFile(
        folder: String = "compressed",
        type: BinaryType = BinaryType.WIN64,
        build: Int = OpenNXT.config.build,
        file: String,
        crc: Long
    ): ByteArray? {
        val path = Constants.CLIENTS_PATH.resolve(build.toString()).resolve(type.name.toLowerCase()).resolve(folder)
            .resolve(file)

        val config = getConfig(folder, type, build) ?: return null
        val info = config.getFiles().firstOrNull { it.name == file } ?: return null
        if (info.crc != crc) return null

        if (!Files.exists(path)) {
            logger.error { "$file not found in $path (it should exist though)" }
            return null
        }

        return Files.readAllBytes(path)
    }

    fun getConfig(
        folder: String = "compressed",
        type: BinaryType = BinaryType.WIN64,
        build: Int = OpenNXT.config.build
    ): ClientConfig? {
        val path = Constants.CLIENTS_PATH.resolve(build.toString()).resolve(type.name.toLowerCase()).resolve(folder)
            .resolve("jav_config.ws")
        if (!Files.exists(path)) {
            logger.error { "jav_config.ws not found in $path (it should exist though)" }
            return null
        }

        return ClientConfig.load(path)
    }

    fun checkFiles(type: String = "compressed", rsaConfig: RsaConfig = OpenNXT.rsaConfig) {
        logger.info { "Checking client files from ${Constants.CLIENTS_PATH}, type '$type'" }
        if (!Files.exists(Constants.CLIENTS_PATH))
            throw FileNotFoundException("${Constants.CLIENTS_PATH} not found. Please `run-tool client-downloader`.")

        val latest = latestBuild()
        if (latest == -1)
            throw FileNotFoundException("Could not find clients/files. Please run `run-tool client-downloader`.")

        if (!Files.exists(Constants.CLIENTS_PATH.resolve(latest.toString())))
            throw FileNotFoundException("${Constants.CLIENTS_PATH.resolve(latest.toString())} not found.")

        val crc = CRC32()
        BinaryType.values().forEach { binaryType ->
            val typePath = Constants.CLIENTS_PATH.resolve(latest.toString()).resolve(binaryType.name.toLowerCase())
            if (!Files.exists(typePath))
                throw FileNotFoundException("Couldn't find binary type $binaryType at $typePath")

            val basePath = typePath.resolve(type)
            if (!Files.exists(basePath))
                throw FileNotFoundException("Couldn't find compressed files. Please run `run-tool client-patcher`")

            val config = ClientConfig.load(basePath.resolve("jav_config.ws"))

            config.getFiles().forEach { downloadInformation ->
                val downloadPath = basePath.resolve(downloadInformation.name)
                if (!Files.exists(downloadPath))
                    throw FileNotFoundException("$downloadPath")

                val decompressed = decompressLZMA(Files.readAllBytes(downloadPath))

                crc.reset()
                crc.update(decompressed)
                if (crc.value != downloadInformation.crc)
                    throw IllegalStateException("CRC mismatch in binary $binaryType file ${downloadInformation.name}")

                val expectedHash = ClientPatcher.generateFileHash(
                    decompressed,
                    rsaConfig.launcher.modulus,
                    rsaConfig.launcher.exponent
                )
                if (expectedHash != downloadInformation.hash)
                    throw IllegalStateException("Hash mismatch in binary $binaryType file ${downloadInformation.name}")
            }
        }

        logger.info("Client files are all OK (checked existence, crc and hash)")
    }

    private fun decompressLZMA(raw: ByteArray): ByteArray =
        LzmaInputStream(ByteArrayInputStream(raw), Decoder()).use { it.readBytes() }
}