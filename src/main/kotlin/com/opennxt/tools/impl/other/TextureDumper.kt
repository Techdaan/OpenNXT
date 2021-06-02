package com.opennxt.tools.impl.other

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.opennxt.Constants
import com.opennxt.filesystem.Container
import com.opennxt.tools.Tool
import io.netty.buffer.Unpooled
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess


class TextureDumper : Tool(name = "texture-dump", help = "This tool dumps textures from a user selected index.") {
    private val idx: Int by option(help = "The index to dump from (default = -1 | all)").choice("dxt" to 52, "png" to 53, "mip" to 54, "etc" to 55, "all" to -1).default(-1)
    private val textureId: Int by option("--tex", help = "The texture to dump (default = -1 | all)").int().default(-1)
    override fun runTool() {
        verifyTextureDirectories()

        when {
            (idx == -1 && textureId == -1) -> {

                for (texIndex in 52..55) {
                    val table = filesystem.getReferenceTable(texIndex)!!
                    table.archives.forEach {
                        decodeTexture(texIndex, it.value.id)
                    }
                }
            }

            (idx != -1 && textureId == -1) -> {

                val table = filesystem.getReferenceTable(idx)!!
                table.archives.forEach {
                    decodeTexture(idx, it.value.id)
                }
            }

            (idx == -1 && textureId != -1) -> {

                val texture = textureId
                for (texIndex in 52..55) {
                    decodeTexture(texIndex, texture)
                }
            }

            (idx != -1 && textureId != -1) -> {

                val texture = textureId
                decodeTexture(idx, texture)
            }

            else -> {
                logger.error { "Invalid option found! | Index: $idx | Texture: $textureId" }
                exitProcess(1)
            }
        }
    }

    private fun decodeTexture(index: Int, textureId: Int) {
        if (!filesystem.exists(index, textureId)) {
            logger.error { "No texture found! | Index: $index - Texture: $textureId | " +
                    "RANGE: ${filesystem.getReferenceTable(index)?.archives?.firstKey()} - " +
                    "${filesystem.getReferenceTable(index)?.archives?.lastKey()}" }
            exitProcess(1)
        }

        val data = Unpooled.wrappedBuffer(Container.decode(filesystem.read(index, textureId)!!).data)
        var raw = ByteArray(data.readableBytes())

        if (index == 54 || index == 55) {
            data.skipBytes(1)
        } else {
            data.skipBytes(5)
            raw = ByteArray(data.readableBytes())
            data.readBytes(raw)
        }

        when (index) {
            52 -> {
                File("${Constants.TEXTURES_PATH_DXT}/${textureId}.dds").writeBytes(raw)
            }
            53 -> {
                val image = ImageIO.read(ByteArrayInputStream(raw))
                ImageIO.write(image, "png", File("${Constants.TEXTURES_PATH_PNG}/${textureId}.png"))
            }
            54 -> {
                for (level in 0 until data.readUnsignedByte().toInt()) {
                    val size = data.readInt()
                    raw = ByteArray(size)
                    data.readBytes(raw)

                    val image = ImageIO.read(ByteArrayInputStream(raw))
                    val file = File("${Constants.TEXTURES_PATH_MIP}/${textureId}/${image.width}x${image.height}.png").also {
                        it.parentFile.mkdirs()
                    }
                    ImageIO.write(image, "png", file)
                }
            }
            55 -> {
                val size = data.readInt()
                raw = ByteArray(size)
                data.readBytes(raw)
                File("${Constants.TEXTURES_PATH_ETC}/${textureId}.ktx").writeBytes(raw)
            }
            else -> {
                logger.error { "Invalid index found! | Index: $index | Texture: $textureId" }
                exitProcess(1)
            }
        }
    }

    private fun verifyTextureDirectories() {
        try {
            File("${Constants.TEXTURES_PATH_DXT}").mkdirs()
            File("${Constants.TEXTURES_PATH_PNG}").mkdirs()
            File("${Constants.TEXTURES_PATH_MIP}").mkdirs()
            File("${Constants.TEXTURES_PATH_ETC}").mkdirs()
        } catch (e: SecurityException) {
            logger.error { "Unable to create required directories!" }
            exitProcess(1)
        }
    }
}
